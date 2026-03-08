package org.artemchik.oldxplorer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.artemchik.oldxplorer.model.FileItem
import org.artemchik.oldxplorer.model.SortBy
import org.artemchik.oldxplorer.repository.FileRepository
import java.util.Stack

class FileViewModel : ViewModel() {

    private val repository = FileRepository()

    private val _files = MutableLiveData<List<FileItem>>()
    val files: LiveData<List<FileItem>> = _files

    private val _currentPath = MutableLiveData<String>()
    val currentPath: LiveData<String> = _currentPath

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val pathHistory = Stack<String>()
    private var currentSortBy = SortBy.NAME
    private var showHiddenFiles = false

    fun loadFiles(path: String) {
        _isLoading.value = true
        _currentPath.value = path

        Thread {
            val allFiles = repository.getFiles(path, currentSortBy)
            val filtered = if (showHiddenFiles) allFiles
                           else allFiles.filter { !it.name.startsWith(".") }
            _files.postValue(filtered)
            _isLoading.postValue(false)
        }.start()
    }

    fun navigateTo(path: String) {
        _currentPath.value?.let { pathHistory.push(it) }
        loadFiles(path)
    }

    fun navigateBack(): Boolean {
        return if (pathHistory.isNotEmpty()) {
            loadFiles(pathHistory.pop())
            true
        } else false
    }

    fun navigateToParent(): Boolean {
        val current = _currentPath.value ?: return false
        val parent = java.io.File(current).parent ?: return false
        pathHistory.push(current)
        loadFiles(parent)
        return true
    }

    fun createFolder(name: String): Boolean {
        val currentDir = _currentPath.value ?: return false
        val result = repository.createFolder(currentDir, name)
        if (result) loadFiles(currentDir)
        return result
    }

    fun deleteFile(path: String): Boolean {
        val result = repository.deleteFile(path)
        _currentPath.value?.let { loadFiles(it) }
        return result
    }

    fun renameFile(oldPath: String, newName: String): Boolean {
        val result = repository.renameFile(oldPath, newName)
        _currentPath.value?.let { loadFiles(it) }
        return result
    }

    fun copyFile(sourcePath: String): Boolean {
        val destDir = _currentPath.value ?: return false
        val result = repository.copyFile(sourcePath, destDir)
        if (result) loadFiles(destDir)
        return result
    }

    fun moveFile(sourcePath: String): Boolean {
        val destDir = _currentPath.value ?: return false
        val result = repository.moveFile(sourcePath, destDir)
        if (result) loadFiles(destDir)
        return result
    }

    fun searchFiles(query: String) {
        _isLoading.value = true
        Thread {
            val currentDir = _currentPath.value ?: return@Thread
            val results = repository.searchFiles(currentDir, query)
            _files.postValue(results)
            _isLoading.postValue(false)
        }.start()
    }

    fun sortFiles(sortBy: SortBy) {
        currentSortBy = sortBy
        _currentPath.value?.let { loadFiles(it) }
    }

    fun toggleHiddenFiles() {
        showHiddenFiles = !showHiddenFiles
        _currentPath.value?.let { loadFiles(it) }
    }

    fun refresh() {
        _currentPath.value?.let { loadFiles(it) }
    }

    fun getCurrentSortBy(): SortBy = currentSortBy
    fun isShowingHidden(): Boolean = showHiddenFiles
}
