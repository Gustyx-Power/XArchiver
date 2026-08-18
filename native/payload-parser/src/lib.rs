use jni::JNIEnv;
use jni::objects::{JClass, JString, JObject};
use jni::sys::{jlong, jobject};
use std::fs::File;
use std::io::Read;

mod parser;
use parser::{PayloadHeader, parse_header, parse_manifest_manual};

#[no_mangle]
pub extern "system" fn Java_id_xms_xarchiver_core_payload_PayloadParserNative_parseHeader(
    mut env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jobject {
    let file_path_str: String = match env.get_string(&file_path) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    let header = match parse_header(&file_path_str) {
        Ok(h) => h,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse header: {}", e));
            return std::ptr::null_mut();
        }
    };

    // Create PayloadHeader Java object
    match create_payload_header_object(&mut env, &header) {
        Ok(obj) => obj.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_id_xms_xarchiver_core_payload_PayloadParserNative_parsePayloadInfo(
    mut env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jobject {
    let file_path_str: String = match env.get_string(&file_path) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    // Parse header first
    let header = match parse_header(&file_path_str) {
        Ok(h) => h,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse header: {}", e));
            return std::ptr::null_mut();
        }
    };

    // Parse manifest manually
    let partitions = match parse_manifest_manual(&file_path_str, &header) {
        Ok(p) => p,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse manifest: {}", e));
            return std::ptr::null_mut();
        }
    };

    // Create PayloadInfo Java object
    match create_payload_info_object(&mut env, &file_path_str, &header, &partitions) {
        Ok(obj) => obj.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_id_xms_xarchiver_core_payload_PayloadParserNative_isPayloadFile(
    mut env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jlong {
    let file_path_str: String = match env.get_string(&file_path) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    match is_payload_file(&file_path_str) {
        Ok(true) => 1,
        _ => 0,
    }
}

fn is_payload_file(path: &str) -> Result<bool, Box<dyn std::error::Error>> {
    let mut file = File::open(path)?;
    let mut magic = [0u8; 4];
    file.read_exact(&mut magic)?;
    Ok(&magic == b"CrAU")
}

fn create_payload_header_object<'local>(
    env: &mut JNIEnv<'local>,
    header: &PayloadHeader,
) -> Result<JObject<'local>, jni::errors::Error> {
    let class = env.find_class("id/xms/xarchiver/core/payload/PayloadHeader")?;
    let obj = env.new_object(
        class,
        "(JJJJ)V",
        &[
            header.version.into(),
            header.manifest_size.into(),
            header.signature_size.into(),
            4096i64.into(), // block_size default
        ],
    )?;
    Ok(obj)
}

fn create_payload_info_object<'local>(
    env: &mut JNIEnv<'local>,
    file_path: &str,
    header: &PayloadHeader,
    partitions: &[parser::Partition],
) -> Result<JObject<'local>, jni::errors::Error> {
    // Create header object
    let header_obj = create_payload_header_object(env, header)?;
    
    // Create ArrayList for partitions
    let array_list_class = env.find_class("java/util/ArrayList")?;
    let partitions_list = env.new_object(array_list_class, "()V", &[])?;
    
    for partition in partitions {
        let partition_obj = create_partition_object(env, partition)?;
        env.call_method(
            &partitions_list,
            "add",
            "(Ljava/lang/Object;)Z",
            &[(&partition_obj).into()],
        )?;
    }
    
    // Get file size
    let file_size = std::fs::metadata(file_path)
        .map(|m| m.len() as i64)
        .unwrap_or(0);
    
    // Create file path string
    let file_path_jstring = env.new_string(file_path)?;
    
    // Create PayloadInfo object
    let class = env.find_class("id/xms/xarchiver/core/payload/PayloadInfo")?;
    let obj = env.new_object(
        class,
        "(Lid/xms/xarchiver/core/payload/PayloadHeader;Ljava/util/List;JLjava/lang/String;)V",
        &[
            (&header_obj).into(),
            (&partitions_list).into(),
            file_size.into(),
            (&file_path_jstring).into(),
        ],
    )?;
    
    Ok(obj)
}

fn create_partition_object<'local>(
    env: &mut JNIEnv<'local>,
    partition: &parser::Partition,
) -> Result<JObject<'local>, jni::errors::Error> {
    let name = env.new_string(&partition.name)?;
    let hash = env.new_string(&partition.hash)?;
    
    // Create empty operations list for now
    let array_list_class = env.find_class("java/util/ArrayList")?;
    let operations_list = env.new_object(array_list_class, "()V", &[])?;
    
    // Get compression type enum
    let compression_type = get_compression_type_enum(env, &partition.compression_type)?;
    
    let class = env.find_class("id/xms/xarchiver/core/payload/PayloadPartition")?;
    let obj = env.new_object(
        class,
        "(Ljava/lang/String;JJLjava/lang/String;Lid/xms/xarchiver/core/payload/CompressionType;JLjava/util/List;)V",
        &[
            (&name).into(),
            partition.compressed_size.into(),
            partition.uncompressed_size.into(),
            (&hash).into(),
            (&compression_type).into(),
            partition.offset.into(),
            (&operations_list).into(),
        ],
    )?;
    
    Ok(obj)
}

fn get_compression_type_enum<'local>(
    env: &mut JNIEnv<'local>,
    compression_type: &str,
) -> Result<JObject<'local>, jni::errors::Error> {
    let class = env.find_class("id/xms/xarchiver/core/payload/CompressionType")?;
    let field_name = match compression_type {
        "XZ" => "REPLACE_XZ",
        "BZIP2" => "REPLACE_BZ",
        "NONE" => "NONE",
        _ => "UNKNOWN",
    };
    
    let field = env.get_static_field(
        class,
        field_name,
        "Lid/xms/xarchiver/core/payload/CompressionType;",
    )?;
    
    Ok(field.l()?)
}
