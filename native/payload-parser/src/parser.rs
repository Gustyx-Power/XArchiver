use std::fs::File;
use std::io::{Read, Seek, SeekFrom};

const MAGIC: &[u8] = b"CrAU";
const HEADER_SIZE: usize = 24;

#[derive(Debug, Clone)]
pub struct PayloadHeader {
    pub version: i64,
    pub manifest_size: i64,
    pub signature_size: i64,
}

#[derive(Debug, Clone)]
pub struct Partition {
    pub name: String,
    pub compressed_size: i64,
    pub uncompressed_size: i64,
    pub hash: String,
    pub compression_type: String,
    pub offset: i64,
}

pub fn parse_header(path: &str) -> Result<PayloadHeader, Box<dyn std::error::Error>> {
    let mut file = File::open(path)?;
    
    // Read magic
    let mut magic = [0u8; 4];
    file.read_exact(&mut magic)?;
    if &magic != MAGIC {
        return Err("Invalid magic number".into());
    }
    
    // Read version (8 bytes, big-endian)
    let mut version_bytes = [0u8; 8];
    file.read_exact(&mut version_bytes)?;
    let version = i64::from_be_bytes(version_bytes);
    
    // Read manifest size (8 bytes, big-endian)
    let mut manifest_size_bytes = [0u8; 8];
    file.read_exact(&mut manifest_size_bytes)?;
    let manifest_size = i64::from_be_bytes(manifest_size_bytes);
    
    // Read signature size (4 bytes, big-endian)
    let mut signature_size_bytes = [0u8; 4];
    file.read_exact(&mut signature_size_bytes)?;
    let signature_size = i32::from_be_bytes(signature_size_bytes) as i64;
    
    Ok(PayloadHeader {
        version,
        manifest_size,
        signature_size,
    })
}

pub fn parse_manifest_manual(path: &str, header: &PayloadHeader) -> Result<Vec<Partition>, Box<dyn std::error::Error>> {
    let mut file = File::open(path)?;
    
    // Seek to manifest
    file.seek(SeekFrom::Start(HEADER_SIZE as u64))?;
    
    // Read manifest bytes
    let mut manifest_bytes = vec![0u8; header.manifest_size as usize];
    file.read_exact(&mut manifest_bytes)?;
    
    // Parse protobuf manually
    parse_protobuf_manifest(&manifest_bytes, header)
}

fn parse_protobuf_manifest(data: &[u8], header: &PayloadHeader) -> Result<Vec<Partition>, Box<dyn std::error::Error>> {
    let mut partitions = Vec::new();
    let mut pos = 0;
    
    // Calculate data offset (after header + manifest + signature)
    let data_offset = (HEADER_SIZE as i64) + header.manifest_size + header.signature_size;
    
    while pos < data.len() {
        // Read field tag
        let (tag, bytes_read) = read_varint(&data[pos..])?;
        pos += bytes_read;
        
        let (field_number, wire_type) = decode_tag(tag);
        
        if field_number == 0 {
            break; // End of message
        }
        
        match wire_type {
            0 => {
                // Varint
                let (_, bytes_read) = read_varint(&data[pos..])?;
                pos += bytes_read;
            }
            2 => {
                // Length-delimited
                let (length, bytes_read) = read_varint(&data[pos..])?;
                pos += bytes_read;
                
                // Field 13 is partitions (repeated PartitionUpdate)
                if field_number == 13 {
                    let partition_data = &data[pos..pos + length as usize];
                    if let Ok(partition) = parse_partition(partition_data, data_offset) {
                        partitions.push(partition);
                    }
                }
                
                pos += length as usize;
            }
            _ => {
                // Skip unknown wire types
                break;
            }
        }
    }
    
    Ok(partitions)
}

fn parse_partition(data: &[u8], data_offset: i64) -> Result<Partition, Box<dyn std::error::Error>> {
    let mut name = String::new();
    let mut uncompressed_size = 0i64;
    let mut hash = String::new();
    let mut operations = Vec::new();
    let mut pos = 0;
    
    while pos < data.len() {
        let (tag, bytes_read) = read_varint(&data[pos..])?;
        pos += bytes_read;
        
        let (field_number, wire_type) = decode_tag(tag);
        
        if field_number == 0 {
            break;
        }
        
        match wire_type {
            0 => {
                // Varint
                let (_, bytes_read) = read_varint(&data[pos..])?;
                pos += bytes_read;
            }
            2 => {
                // Length-delimited
                let (length, bytes_read) = read_varint(&data[pos..])?;
                pos += bytes_read;
                
                match field_number {
                    1 => {
                        // partition_name
                        name = String::from_utf8_lossy(&data[pos..pos + length as usize]).to_string();
                    }
                    3 => {
                        // operations (repeated InstallOperation)
                        let op_data = &data[pos..pos + length as usize];
                        if let Ok(op) = parse_operation(op_data) {
                            operations.push(op);
                        }
                    }
                    5 => {
                        // new_partition_info
                        let info_data = &data[pos..pos + length as usize];
                        if let Ok((size, h)) = parse_partition_info(info_data) {
                            uncompressed_size = size;
                            hash = h;
                        }
                    }
                    _ => {}
                }
                
                pos += length as usize;
            }
            _ => break,
        }
    }
    
    // Calculate compressed size and detect compression type
    let mut compressed_size = 0i64;
    let mut compression_type = String::from("NONE");
    let mut first_offset = 0i64;
    
    for (i, op) in operations.iter().enumerate() {
        compressed_size += op.data_length;
        if i == 0 {
            first_offset = data_offset + op.data_offset;
        }
        
        // Detect compression from operation type
        match op.op_type {
            8 => compression_type = String::from("XZ"),
            1 => compression_type = String::from("BZIP2"),
            _ => {}
        }
    }
    
    Ok(Partition {
        name,
        compressed_size,
        uncompressed_size,
        hash,
        compression_type,
        offset: first_offset,
    })
}

#[derive(Debug)]
struct Operation {
    op_type: u32,
    data_offset: i64,
    data_length: i64,
}

fn parse_operation(data: &[u8]) -> Result<Operation, Box<dyn std::error::Error>> {
    let mut op_type = 0u32;
    let mut data_offset = 0i64;
    let mut data_length = 0i64;
    let mut pos = 0;
    
    while pos < data.len() {
        let (tag, bytes_read) = read_varint(&data[pos..])?;
        pos += bytes_read;
        
        let (field_number, wire_type) = decode_tag(tag);
        
        if field_number == 0 {
            break;
        }
        
        match wire_type {
            0 => {
                // Varint
                let (value, bytes_read) = read_varint(&data[pos..])?;
                pos += bytes_read;
                
                match field_number {
                    1 => op_type = value as u32,      // type
                    2 => data_offset = value as i64,  // data_offset
                    3 => data_length = value as i64,  // data_length
                    _ => {}
                }
            }
            2 => {
                // Length-delimited (skip)
                let (length, bytes_read) = read_varint(&data[pos..])?;
                pos += bytes_read;
                pos += length as usize;
            }
            _ => break,
        }
    }
    
    Ok(Operation {
        op_type,
        data_offset,
        data_length,
    })
}

fn parse_partition_info(data: &[u8]) -> Result<(i64, String), Box<dyn std::error::Error>> {
    let mut size = 0i64;
    let mut hash = String::new();
    let mut pos = 0;
    
    while pos < data.len() {
        let (tag, bytes_read) = read_varint(&data[pos..])?;
        pos += bytes_read;
        
        let (field_number, wire_type) = decode_tag(tag);
        
        if field_number == 0 {
            break;
        }
        
        match wire_type {
            0 => {
                // Varint
                let (value, bytes_read) = read_varint(&data[pos..])?;
                pos += bytes_read;
                
                if field_number == 1 {
                    size = value as i64;
                }
            }
            2 => {
                // Length-delimited
                let (length, bytes_read) = read_varint(&data[pos..])?;
                pos += bytes_read;
                
                if field_number == 2 {
                    // hash bytes
                    hash = data[pos..pos + length as usize]
                        .iter()
                        .map(|b| format!("{:02x}", b))
                        .collect();
                }
                
                pos += length as usize;
            }
            _ => break,
        }
    }
    
    Ok((size, hash))
}

fn read_varint(data: &[u8]) -> Result<(u64, usize), Box<dyn std::error::Error>> {
    let mut result = 0u64;
    let mut shift = 0;
    let mut bytes_read = 0;
    
    for &byte in data.iter().take(10) {
        bytes_read += 1;
        result |= ((byte & 0x7F) as u64) << shift;
        
        if byte & 0x80 == 0 {
            return Ok((result, bytes_read));
        }
        
        shift += 7;
    }
    
    Err("Varint too long".into())
}

fn decode_tag(tag: u64) -> (u64, u8) {
    let field_number = tag >> 3;
    let wire_type = (tag & 0x07) as u8;
    (field_number, wire_type)
}
