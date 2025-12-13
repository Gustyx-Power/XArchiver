package id.xms.xarchiver.core

import androidx.compose.runtime.mutableStateListOf
import java.io.File

/**
 * Manager for multi-file selection in explorer
 */
class SelectionManager {
    
    private val _selectedPaths = mutableStateListOf<String>()
    val selectedPaths: List<String> get() = _selectedPaths
    
    val selectedCount: Int get() = _selectedPaths.size
    
    val isSelecting: Boolean get() = _selectedPaths.isNotEmpty()
    
    /**
     * Toggle selection for a file
     */
    fun toggleSelection(path: String) {
        if (_selectedPaths.contains(path)) {
            _selectedPaths.remove(path)
        } else {
            _selectedPaths.add(path)
        }
    }
    
    /**
     * Check if a path is selected
     */
    fun isSelected(path: String): Boolean = _selectedPaths.contains(path)
    
    /**
     * Select all files in a list
     */
    fun selectAll(paths: List<String>) {
        _selectedPaths.clear()
        _selectedPaths.addAll(paths)
    }
    
    /**
     * Clear all selections
     */
    fun clearSelection() {
        _selectedPaths.clear()
    }
    
    /**
     * Reverse selection (select unselected, unselect selected)
     */
    fun reverseSelection(allPaths: List<String>) {
        val currentlySelected = _selectedPaths.toSet()
        val newSelection = allPaths.filter { it !in currentlySelected }
        _selectedPaths.clear()
        _selectedPaths.addAll(newSelection)
    }
    
    /**
     * Select all files with the same extension
     */
    fun selectSameType(allPaths: List<String>, referencePath: String) {
        val referenceFile = File(referencePath)
        if (referenceFile.isDirectory) {
            // Select all directories
            val directories = allPaths.filter { File(it).isDirectory }
            _selectedPaths.clear()
            _selectedPaths.addAll(directories)
        } else {
            // Select all files with same extension
            val extension = referenceFile.extension.lowercase()
            val sameType = allPaths.filter { 
                val file = File(it)
                !file.isDirectory && file.extension.lowercase() == extension
            }
            _selectedPaths.clear()
            _selectedPaths.addAll(sameType)
        }
    }
    
    /**
     * Get total size of selected files
     */
    fun getTotalSelectedSize(): Long {
        return _selectedPaths.sumOf { path ->
            val file = File(path)
            if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
    }
    
    private fun calculateDirectorySize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
}
