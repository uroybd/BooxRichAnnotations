package me.utsob.booxrichannotation

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BookAdapter(
    private val booksWithAnnotations: List<BookWithAnnotations>,
    private val lifecycleScope: CoroutineScope
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {
    
    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.book_title)
        val authorTextView: TextView = view.findViewById(R.id.book_author)
        val annotationCountTextView: TextView = view.findViewById(R.id.annotation_count)
        val saveButton: ImageButton = view.findViewById(R.id.btn_save)
        val shareButton: ImageButton = view.findViewById(R.id.btn_share)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val bookWithAnnotations = booksWithAnnotations[position]
        val book = bookWithAnnotations.book
        val annotations = bookWithAnnotations.annotations
        
        holder.titleTextView.text = book.getDisplayTitle()
        holder.authorTextView.text = book.getDisplayAuthors()
        
        // Show annotation count
        val count = annotations.size
        holder.annotationCountTextView.text = if (count > 0) {
            "$count annotation${if (count != 1) "s" else ""}"
        } else {
            "No annotations"
        }
        
        // Disable buttons if no annotations
        val hasAnnotations = count > 0
        holder.saveButton.isEnabled = hasAnnotations
        holder.shareButton.isEnabled = hasAnnotations
        holder.saveButton.alpha = if (hasAnnotations) 1.0f else 0.4f
        holder.shareButton.alpha = if (hasAnnotations) 1.0f else 0.4f
        
        // Save button - downloads and shows open dialog
        holder.saveButton.setOnClickListener {
            if (hasAnnotations) {
                saveAnnotationsAsJson(holder.itemView.context, book, annotations)
            }
        }
        
        // Share button
        holder.shareButton.setOnClickListener {
            if (hasAnnotations) {
                shareAnnotationsAsJson(holder.itemView.context, book, annotations)
            }
        }
    }
    
    private fun saveAnnotationsAsJson(context: Context, book: BookMetadata, annotations: List<Annotation>) {
        lifecycleScope.launch {
            try {
                val jsonContent = buildJsonContent(book, annotations)
                val fileName = generateFileName(book)
                
                val fileUri = withContext(Dispatchers.IO) {
                    saveJsonFileAndGetUri(context, fileName, jsonContent)
                }
                
                if (fileUri != null) {
                    withContext(Dispatchers.Main) {
                        showOpenFileDialog(context, fileName, fileUri)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BookAdapter", "Error saving annotations", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showOpenFileDialog(context: Context, fileName: String, fileUri: Uri) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("File Saved")
            .setMessage("$fileName saved to Downloads folder.\n\nWould you like to open it?")
            .setPositiveButton("Open") { dialog, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, "application/json")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No app to open JSON files", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog)
        dialog.show()
        
        // Style the buttons after showing
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            backgroundTintList = null
            setBackgroundResource(R.drawable.bg_dialog_button)
            setTextColor(android.graphics.Color.BLACK)
            setPadding(32, 16, 32, 16)
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            backgroundTintList = null
            setBackgroundResource(R.drawable.bg_dialog_button)
            setTextColor(android.graphics.Color.BLACK)
            setPadding(32, 16, 32, 16)
        }
    }
    
    private fun shareAnnotationsAsJson(context: Context, book: BookMetadata, annotations: List<Annotation>) {
        lifecycleScope.launch {
            try {
                val jsonContent = buildJsonContent(book, annotations)
                val fileName = generateFileName(book)
                
                val fileUri = withContext(Dispatchers.IO) {
                    saveToTempAndGetUri(context, fileName, jsonContent)
                }
                
                if (fileUri != null) {
                    withContext(Dispatchers.Main) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            putExtra(Intent.EXTRA_SUBJECT, "${book.getDisplayTitle()} - Annotations")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share annotations"))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BookAdapter", "Error sharing annotations", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun buildJsonContent(book: BookMetadata, annotations: List<Annotation>): String {
        // Sort annotations by page number first, then by location in document
        val sortedAnnotations = annotations.sortedWith(
            compareBy({ it.pageNumber }, { it.locationBeginInt })
        )
        
        val jsonArray = JSONArray()
        
        for (annotation in sortedAnnotations) {
            val jsonObj = JSONObject()
            
            annotation.quote?.let { jsonObj.put("quote", it) }
            annotation.pageNumber?.let { jsonObj.put("pageNumber", it) }
            annotation.chapter?.let { if (it != "NULL") jsonObj.put("chapter", it) }
            annotation.createdAt?.let { jsonObj.put("createdAt", it) }
            
            // Convert color integer to hex
            annotation.color?.let { colorInt ->
                try {
                    val colorLong = colorInt.toLong() and 0xFFFFFFFFL
                    val r = (colorLong shr 16) and 0xFF
                    val g = (colorLong shr 8) and 0xFF
                    val b = colorLong and 0xFF
                    jsonObj.put("color", "#%02x%02x%02x".format(r, g, b))
                } catch (e: Exception) {
                    Log.w("BookAdapter", "Error converting color: $colorInt", e)
                }
            }
            
            // Convert shape to style name
            annotation.shape?.let { shape ->
                val style = when (shape) {
                    0 -> "highlight"
                    1 -> "underline"
                    2 -> "dashed"
                    3 -> "wavy"
                    4 -> "redact"
                    5 -> "mute"
                    else -> "unknown"
                }
                jsonObj.put("style", style)
            }
            
            annotation.note?.let { if (it.isNotBlank() && it != "NULL") jsonObj.put("note", it) }
            
            jsonArray.put(jsonObj)
        }
        
        val rootObject = JSONObject()
        rootObject.put("title", book.getDisplayTitle())
        rootObject.put("authors", book.getDisplayAuthors())
        book.idString?.let {
            val format = when {
                it.endsWith(".epub", ignoreCase = true) -> "epub"
                it.endsWith(".mobi", ignoreCase = true) -> "mobi"
                it.endsWith(".azw", ignoreCase = true) -> "azw"
                it.endsWith(".azw3", ignoreCase = true) -> "azw3"
                else -> "unknown"
            }
            rootObject.put("format", format)
        }
        rootObject.put("annotations", jsonArray)
        
        return rootObject.toString(2)
    }
    
    private fun generateFileName(book: BookMetadata): String {
        val sanitizedTitle = book.getDisplayTitle()
            .replace(Regex("[^a-zA-Z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "_")
            .take(50)
        return "${sanitizedTitle}_annotations.json"
    }
    
    private fun saveJsonFileAndGetUri(context: Context, fileName: String, content: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: Use MediaStore API
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(it, contentValues, null, null)
                it
            }
        } else {
            // Android 9 and below: Direct file access
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.writeText(content)
                
                // Return file URI for Android 9 and below
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                Log.e("BookAdapter", "Error saving file on Android 9-", e)
                null
            }
        }
    }
    
    private fun saveToTempAndGetUri(context: Context, fileName: String, content: String): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "shared_annotations")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val file = File(cacheDir, fileName)
            file.writeText(content)
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("BookAdapter", "Error creating temp file", e)
            null
        }
    }
    
    override fun getItemCount() = booksWithAnnotations.size
}

