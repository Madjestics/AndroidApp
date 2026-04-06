package com.example.movieandroid.util

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtils {
    fun copyUriToCache(context: Context, uri: Uri): File {
        val cacheFile = File.createTempFile("upload_", ".mp4", context.cacheDir)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Не удалось открыть файл" }
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return cacheFile
    }
}
