package com.yad.musicstudio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * ShareHelper — Share audio ke social media.
 */
object ShareHelper {

    fun shareAudio(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "🎵 Dibuat dengan YAD Music Studio!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Audio"))
        } catch (e: Exception) {
            android.util.Log.e("ShareHelper", "share error", e)
        }
    }

    fun shareToTikTok(context: Context, filePath: String) {
        try {
            val intent = Intent("com.zhiliaoapp.musically").apply {
                setPackage("com.zhiliaoapp.musically")
                putExtra("videoPath", filePath)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context,
                "TikTok tidak terinstall", android.widget.Toast.LENGTH_SHORT).show()
            shareAudio(context, filePath)
        }
    }

    fun shareToInstagram(context: Context, filePath: String) {
        try {
            val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setPackage("com.instagram.android")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context,
                "Instagram tidak terinstall", android.widget.Toast.LENGTH_SHORT).show()
            shareAudio(context, filePath)
        }
    }

    fun saveToDownloads(context: Context, filePath: String): Boolean {
        return try {
            val src = File(filePath)
            if (!src.exists()) return false

            val downloads = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_MUSIC
            )
            val dest = File(downloads, "YadMusicStudio")
            if (!dest.exists()) dest.mkdirs()

            val out = File(dest, src.name)
            src.copyTo(out, overwrite = true)
            true
        } catch (e: Exception) {
            android.util.Log.e("ShareHelper", "saveToDownloads error", e)
            false
        }
    }
}
