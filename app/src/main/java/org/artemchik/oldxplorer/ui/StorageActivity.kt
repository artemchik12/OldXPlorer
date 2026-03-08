package org.artemchik.oldxplorer.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import org.artemchik.oldxplorer.R
import org.artemchik.oldxplorer.databinding.ActivityStorageBinding
import org.artemchik.oldxplorer.utils.StorageHelper

class StorageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStorageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        binding = ActivityStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackStorage.setOnClickListener { finish() }

        loadStorageVolumes()
    }

    private fun loadStorageVolumes() {
        val helper = StorageHelper()
        val volumes = helper.getAllStorageVolumes()
        val container = binding.storageContainer

        container.removeAllViews()

        volumes.forEach { volume ->
            val card = createStorageCard(volume)
            container.addView(card)
        }

        if (volumes.size <= 1) {
            val tv = TextView(this).apply {
                text = "No external SD card detected"
                setPadding(16, 48, 16, 16)
                gravity = Gravity.CENTER
                setTextColor(0xFF999999.toInt())
                textSize = 14f
            }
            container.addView(tv)
        }
    }

    private fun createStorageCard(volume: org.artemchik.oldxplorer.utils.StorageVolume): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(0xFFFFFFFF.toInt())

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(24, 12, 24, 12)
            layoutParams = params

            // Title row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                addView(ImageView(context).apply {
                    setImageResource(
                        if (volume.isRemovable) R.drawable.ic_sd_card else R.drawable.ic_storage
                    )
                    layoutParams = LinearLayout.LayoutParams(64, 64)
                })

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(24, 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                    addView(TextView(context).apply {
                        text = volume.name
                        textSize = 17f
                        setTextColor(0xFF333333.toInt())
                    })

                    addView(TextView(context).apply {
                        text = volume.path
                        textSize = 12f
                        setTextColor(0xFF999999.toInt())
                    })
                })
            })

            // Progress bar
            addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = volume.getUsagePercent()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 16, 0, 8) }
            })

            // Space info
            addView(TextView(context).apply {
                text = "${volume.getUsedFormatted()} used of ${volume.getTotalFormatted()} (${volume.getFreeFormatted()} free)"
                textSize = 13f
                setTextColor(0xFF666666.toInt())
            })

            setOnClickListener {
                val intent = Intent(context, MainActivity::class.java)
                intent.putExtra("START_PATH", volume.path)
                startActivity(intent)
            }

            isClickable = true
            isFocusable = true
        }
    }
}
