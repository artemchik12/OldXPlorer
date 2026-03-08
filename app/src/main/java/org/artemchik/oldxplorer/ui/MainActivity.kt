package org.artemchik.oldxplorer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.artemchik.oldxplorer.R
import org.artemchik.oldxplorer.databinding.ActivityMainBinding
import org.artemchik.oldxplorer.model.FileItem
import org.artemchik.oldxplorer.model.SortBy
import org.artemchik.oldxplorer.utils.FileUtils
import org.artemchik.oldxplorer.viewmodel.FileViewModel
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: FileViewModel
    private lateinit var adapter: FileAdapter

    private var clipboardFile: FileItem? = null
    private var clipboardMode: String? = null  // "copy" or "move"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(FileViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupButtons()

        val startPath = intent.getStringExtra("START_PATH")
            ?: Environment.getExternalStorageDirectory().absolutePath

        viewModel.loadFiles(startPath)
    }

    private fun setupRecyclerView() {
        adapter = FileAdapter(
            onFileClick = { fileItem ->
                if (fileItem.isDirectory) {
                    viewModel.navigateTo(fileItem.path)
                } else {
                    openFile(fileItem)
                }
            },
            onFileLongClick = { fileItem ->
                showFileOptionsDialog(fileItem)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.files.observe(this) { files ->
            adapter.updateFiles(files)
            binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
            binding.tvFileCount.text = "${files.size} items"
        }

        viewModel.currentPath.observe(this) { path ->
            binding.tvCurrentPath.text = path
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            viewModel.navigateBack()
        }

        binding.btnUp.setOnClickListener {
            viewModel.navigateToParent()
        }

        binding.btnHome.setOnClickListener {
            val homePath = Environment.getExternalStorageDirectory().absolutePath
            viewModel.loadFiles(homePath)
        }

        binding.btnNewFolder.setOnClickListener {
            showCreateFolderDialog()
        }

        binding.btnSearch.setOnClickListener {
            showSearchDialog()
        }

        binding.btnSort.setOnClickListener {
            showSortDialog()
        }

        binding.btnStorage.setOnClickListener {
            startActivity(Intent(this, StorageActivity::class.java))
        }

        binding.btnMore.setOnClickListener {
            showMoreOptionsDialog()
        }

        binding.btnPaste.setOnClickListener {
            performPaste()
        }
    }

    @Suppress("DEPRECATION")
    private fun openFile(fileItem: FileItem) {
        val file = File(fileItem.path)
        val uri = Uri.fromFile(file)

        val mimeType = fileItem.getMimeType()

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFileOptionsDialog(fileItem: FileItem) {
        val options = arrayOf(
            "Copy", "Move", "Rename", "Delete", "Details"
        )

        AlertDialog.Builder(this)
            .setTitle(fileItem.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startClipboard(fileItem, "copy")
                    1 -> startClipboard(fileItem, "move")
                    2 -> showRenameDialog(fileItem)
                    3 -> confirmDelete(fileItem)
                    4 -> showFileDetails(fileItem)
                }
            }.show()
    }

    private fun startClipboard(fileItem: FileItem, mode: String) {
        clipboardFile = fileItem
        clipboardMode = mode
        binding.btnPaste.visibility = View.VISIBLE
        binding.tvClipboard.visibility = View.VISIBLE
        binding.tvClipboard.text = "${mode.uppercase()}: ${fileItem.name}"
        Toast.makeText(this, "Navigate to destination folder and tap Paste", Toast.LENGTH_LONG).show()
    }

    private fun performPaste() {
        val file = clipboardFile ?: return
        val mode = clipboardMode ?: return

        val success = when (mode) {
            "copy" -> viewModel.copyFile(file.path)
            "move" -> viewModel.moveFile(file.path)
            else -> false
        }

        if (success) {
            Toast.makeText(this, "${mode.capitalize()} successful!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "${mode.capitalize()} failed!", Toast.LENGTH_SHORT).show()
        }

        clipboardFile = null
        clipboardMode = null
        binding.btnPaste.visibility = View.GONE
        binding.tvClipboard.visibility = View.GONE
    }

    private fun showCreateFolderDialog() {
        val input = EditText(this)
        input.hint = "Folder name"
        input.setPadding(48, 32, 48, 32)

        AlertDialog.Builder(this)
            .setTitle("Create New Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (viewModel.createFolder(name)) {
                        Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to create folder", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(fileItem: FileItem) {
        val input = EditText(this)
        input.setText(fileItem.name)
        input.setPadding(48, 32, 48, 32)
        input.setSelection(
            0,
            if (fileItem.isDirectory) fileItem.name.length
            else fileItem.name.lastIndexOf('.').let { if (it > 0) it else fileItem.name.length }
        )

        AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != fileItem.name) {
                    if (viewModel.renameFile(fileItem.path, newName)) {
                        Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(fileItem: FileItem) {
        val type = if (fileItem.isDirectory) "folder and all its contents" else "file"
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete this $type?\n\n${fileItem.name}")
            .setPositiveButton("Delete") { _, _ ->
                if (viewModel.deleteFile(fileItem.path)) {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFileDetails(fileItem: FileItem) {
        val details = buildString {
            appendLine("Name: ${fileItem.name}")
            appendLine("Path: ${fileItem.path}")
            appendLine("Size: ${fileItem.getFormattedSize()}")
            appendLine("Type: ${if (fileItem.isDirectory) "Folder" else fileItem.extension.uppercase()}")
            appendLine("Modified: ${FileUtils.formatDate(fileItem.lastModified)}")
            if (!fileItem.isDirectory) {
                appendLine("MIME: ${fileItem.getMimeType()}")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("File Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSearchDialog() {
        val input = EditText(this)
        input.hint = "Search files..."
        input.setPadding(48, 32, 48, 32)

        AlertDialog.Builder(this)
            .setTitle("Search")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.searchFiles(query)
                    Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf("Name", "Size (largest first)", "Date (newest first)", "Type")
        val currentSort = viewModel.getCurrentSortBy().ordinal

        AlertDialog.Builder(this)
            .setTitle("Sort by")
            .setSingleChoiceItems(options, currentSort) { dialog, which ->
                viewModel.sortFiles(SortBy.values()[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMoreOptionsDialog() {
        val hiddenLabel = if (viewModel.isShowingHidden()) "Hide hidden files" else "Show hidden files"
        val options = arrayOf(hiddenLabel, "Refresh", "About")

        AlertDialog.Builder(this)
            .setTitle("More Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        viewModel.toggleHiddenFiles()
                        val msg = if (viewModel.isShowingHidden()) "Showing hidden files" else "Hidden files hidden"
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    1 -> viewModel.refresh()
                    2 -> showAboutDialog()
                }
            }.show()
    }

    private fun showAboutDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        val logo = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(180, 180)
        }

        val title = TextView(this).apply {
            text = "OldXPlorer"
            textSize = 22f
            setPadding(0, 32, 0, 8)
            setTextColor(0xFF333333.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val version = TextView(this).apply {
            text = "Version 1.0\nTargeting Android 4.2.2"
            gravity = android.view.Gravity.CENTER
            setTextColor(0xFF666666.toInt())
        }

        layout.addView(logo)
        layout.addView(title)
        layout.addView(version)

        AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Great!", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!viewModel.navigateBack()) {
            super.onBackPressed()
        }
    }
}
