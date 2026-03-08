package org.artemchik.oldxplorer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.artemchik.oldxplorer.R
import org.artemchik.oldxplorer.model.FileItem
import org.artemchik.oldxplorer.utils.FileUtils

class FileAdapter(
    private val onFileClick: (FileItem) -> Unit,
    private val onFileLongClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private var files = listOf<FileItem>()

    fun updateFiles(newFiles: List<FileItem>) {
        files = newFiles
        notifyDataSetChanged()
    }

    fun getFiles(): List<FileItem> = files

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvFileName)
        private val tvInfo: TextView = itemView.findViewById(R.id.tvFileInfo)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(fileItem: FileItem) {
            tvName.text = fileItem.name
            ivIcon.setImageResource(fileItem.getFileIcon())

            tvInfo.text = if (fileItem.isDirectory) {
                "Folder"
            } else {
                fileItem.getFormattedSize()
            }

            tvDate.text = FileUtils.formatDate(fileItem.lastModified)

            itemView.setOnClickListener { onFileClick(fileItem) }
            itemView.setOnLongClickListener {
                onFileLongClick(fileItem)
                true
            }

            // Dim hidden files
            val alpha = if (fileItem.name.startsWith(".")) 0.5f else 1.0f
            itemView.alpha = alpha
        }
    }
}
