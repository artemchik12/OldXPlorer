package org.artemchik.oldxplorer.utils

import android.os.Environment
import android.os.StatFs
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Scanner

data class StorageVolume(
    val name: String,
    val path: String,
    val isRemovable: Boolean,
    val totalSpace: Long,
    val freeSpace: Long,
    val usedSpace: Long
) {
    fun getTotalFormatted(): String = formatSize(totalSpace)
    fun getFreeFormatted(): String = formatSize(freeSpace)
    fun getUsedFormatted(): String = formatSize(usedSpace)

    fun getUsagePercent(): Int {
        if (totalSpace == 0L) return 0
        return ((usedSpace.toDouble() / totalSpace) * 100).toInt()
    }

    companion object {
        fun formatSize(bytes: Long): String {
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1 -> String.format("%.2f GB", gb)
                mb >= 1 -> String.format("%.2f MB", mb)
                kb >= 1 -> String.format("%.2f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}

class StorageHelper {

    fun getAllStorageVolumes(): List<StorageVolume> {
        val volumes = mutableListOf<StorageVolume>()

        val internalPath = Environment.getExternalStorageDirectory().absolutePath
        volumes.add(createStorageVolume("Internal Storage", internalPath, false))

        val sdCards = findExternalSDCards()
        sdCards.forEach { path ->
            if (path != internalPath && File(path).exists() && File(path).canRead()) {
                volumes.add(createStorageVolume("SD Card", path, true))
            }
        }

        return volumes
    }

    private fun findExternalSDCards(): List<String> {
        val paths = mutableSetOf<String>()
        paths.addAll(getFromMounts())
        paths.addAll(getFromCommonPaths())
        paths.addAll(getFromVold())
        paths.addAll(getFromEnv())

        return paths.filter { path ->
            val file = File(path)
            file.exists() && file.isDirectory && file.canRead()
        }
    }

    private fun getFromMounts(): Set<String> {
        val paths = mutableSetOf<String>()
        try {
            val reader = BufferedReader(FileReader("/proc/mounts"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(" ")
                if (parts.size >= 3) {
                    val mountPoint = parts[1]
                    val fs = parts[2]
                    if ((fs == "vfat" || fs == "fuse" || fs == "sdcardfs" || fs == "ext4") &&
                        (mountPoint.contains("storage") || mountPoint.contains("sdcard") ||
                         mountPoint.contains("extsd") || mountPoint.contains("external")) &&
                        !mountPoint.contains("asec") && !mountPoint.contains("obb") &&
                        !mountPoint.contains("secure")
                    ) {
                        paths.add(mountPoint)
                    }
                }
            }
            reader.close()
        } catch (e: Exception) { }
        return paths
    }

    private fun getFromCommonPaths(): Set<String> {
        val paths = mutableSetOf<String>()
        val commonPaths = arrayOf(
            "/storage/extSdCard", "/storage/sdcard1", "/storage/external_sd",
            "/storage/external_SD", "/storage/MicroSD", "/storage/ext_sd",
            "/storage/sdcard2", "/storage/removable/sdcard1",
            "/mnt/extsd", "/mnt/external_sd", "/mnt/sdcard2",
            "/mnt/sdcard/external_sd", "/mnt/sdcard-ext", "/mnt/external",
            "/mnt/extSdCard", "/mnt/media_rw/sdcard1",
            "/storage/emulated/0", "/storage/sdcard0",
            "/storage/usbdisk", "/storage/UsbDriveA",
            "/mnt/usb_storage", "/mnt/UsbDriveA"
        )
        commonPaths.forEach { path ->
            val file = File(path)
            if (file.exists() && file.isDirectory && file.canRead()) {
                val contents = file.listFiles()
                if (contents != null && contents.isNotEmpty()) {
                    paths.add(path)
                }
            }
        }
        return paths
    }

    private fun getFromVold(): Set<String> {
        val paths = mutableSetOf<String>()
        try {
            val vold = File("/system/etc/vold.fstab")
            if (vold.exists()) {
                val scanner = Scanner(vold)
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine().trim()
                    if (line.startsWith("dev_mount") || line.startsWith("fuse_mount")) {
                        val parts = line.split(" ")
                        if (parts.size >= 3) paths.add(parts[2])
                    }
                }
                scanner.close()
            }
        } catch (e: Exception) { }
        return paths
    }

    private fun getFromEnv(): Set<String> {
        val paths = mutableSetOf<String>()
        try {
            System.getenv("SECONDARY_STORAGE")?.split(":")?.forEach { paths.add(it) }
            System.getenv("EXTERNAL_STORAGE")?.let { paths.add(it) }
        } catch (e: Exception) { }
        return paths
    }

    @Suppress("DEPRECATION")
    private fun createStorageVolume(
        name: String, path: String, isRemovable: Boolean
    ): StorageVolume {
        return try {
            val stat = StatFs(path)
            val blockSize = stat.blockSize.toLong()
            val totalBlocks = stat.blockCount.toLong()
            val availableBlocks = stat.availableBlocks.toLong()
            val totalSpace = totalBlocks * blockSize
            val freeSpace = availableBlocks * blockSize
            StorageVolume(name, path, isRemovable, totalSpace, freeSpace, totalSpace - freeSpace)
        } catch (e: Exception) {
            StorageVolume(name, path, isRemovable, 0, 0, 0)
        }
    }
}
