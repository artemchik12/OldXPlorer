package org.artemchik.oldxplorer.model

import org.artemchik.oldxplorer.R

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String
) {
    fun getFormattedSize(): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$size B"
        }
    }

    fun getFileIcon(): Int {
        if (isDirectory) return R.drawable.ic_folder
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> R.drawable.ic_image
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "3gp" -> R.drawable.ic_video
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a" -> R.drawable.ic_music
            "pdf" -> R.drawable.ic_pdf
            "txt", "doc", "docx", "rtf", "odt" -> R.drawable.ic_document
            "zip", "rar", "7z", "tar", "gz" -> R.drawable.ic_archive
            "apk" -> R.drawable.ic_apk
            "xml", "json", "html", "css", "js", "py", "java", "kt" -> R.drawable.ic_code
            else -> R.drawable.ic_file
        }
    }

    fun getMimeType(): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "3gp" -> "video/3gpp"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "html" -> "text/html"
            "xml" -> "text/xml"
            "json" -> "application/json"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "apk" -> "application/vnd.android.package-archive"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            else -> "*/*"
        }
    }
}
