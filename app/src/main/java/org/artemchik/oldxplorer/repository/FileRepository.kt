package org.artemchik.oldxplorer.repository

import org.artemchik.oldxplorer.model.FileItem
import org.artemchik.oldxplorer.model.SortBy
import java.io.File

class FileRepository {

    fun getFiles(directoryPath: String, sortBy: SortBy = SortBy.NAME): List<FileItem> {
        val directory = File(directoryPath)

        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }

        val files = directory.listFiles() ?: return emptyList()

        val fileItems = files.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = if (file.isFile) file.length() else 0L,
                lastModified = file.lastModified(),
                extension = file.extension
            )
        }

        return fileItems.sortedWith(
            compareByDescending<FileItem> { it.isDirectory }
                .then(
                    when (sortBy) {
                        SortBy.NAME -> compareBy { it.name.lowercase() }
                        SortBy.SIZE -> compareByDescending { it.size }
                        SortBy.DATE -> compareByDescending { it.lastModified }
                        SortBy.TYPE -> compareBy { it.extension.lowercase() }
                    }
                )
        )
    }

    fun createFolder(parentPath: String, folderName: String): Boolean {
        val folder = File(parentPath, folderName)
        return if (!folder.exists()) folder.mkdir() else false
    }

    fun deleteFile(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun renameFile(oldPath: String, newName: String): Boolean {
        val oldFile = File(oldPath)
        val newFile = File(oldFile.parent, newName)
        return if (!newFile.exists()) oldFile.renameTo(newFile) else false
    }

    fun copyFile(sourcePath: String, destDirPath: String): Boolean {
        return try {
            val source = File(sourcePath)
            val destDir = File(destDirPath)
            val dest = File(destDir, source.name)

            if (source.absolutePath == dest.absolutePath) return false

            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = false)
            } else {
                source.copyTo(dest, overwrite = false)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun moveFile(sourcePath: String, destDirPath: String): Boolean {
        return if (copyFile(sourcePath, destDirPath)) {
            deleteFile(sourcePath)
        } else {
            false
        }
    }

    fun searchFiles(directoryPath: String, query: String): List<FileItem> {
        val dir = File(directoryPath)
        val results = mutableListOf<FileItem>()

        try {
            dir.walkTopDown()
                .maxDepth(5)
                .filter { it.name.contains(query, ignoreCase = true) }
                .take(200)
                .forEach { file ->
                    results.add(
                        FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = file.isDirectory,
                            size = if (file.isFile) file.length() else 0L,
                            lastModified = file.lastModified(),
                            extension = file.extension
                        )
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    fun getDirectorySize(dirPath: String): Long {
        return try {
            File(dirPath).walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    fun getFileCount(dirPath: String): Int {
        return try {
            File(dirPath).listFiles()?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
