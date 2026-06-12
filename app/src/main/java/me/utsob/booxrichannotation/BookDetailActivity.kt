package me.utsob.booxrichannotation

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BookDetailActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bookTitleText: TextView
    private lateinit var annotationCountText: TextView
    private lateinit var downloadButton: Button
    private lateinit var progressBar: ProgressBar
    
    private var bookMetadata: BookMetadata? = null
    private var annotations: List<Annotation> = emptyList()
    private var allUuids: List<String>? = null
    
    companion object {
        const val EXTRA_ALL_UUIDS = "all_uuids"
        const val EXTRA_BOOK_TITLE = "book_title"
        const val EXTRA_BOOK_AUTHORS = "book_authors"
        const val EXTRA_BOOK_FILE_PATH = "book_file_path"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Disable animations for e-ink
        window.setWindowAnimations(0)
        overridePendingTransition(0, 0)
        
        setContentView(R.layout.activity_book_detail)
        
        toolbar = findViewById(R.id.toolbar)
        bookTitleText = findViewById(R.id.book_title_text)
        annotationCountText = findViewById(R.id.annotation_count_text)
        downloadButton = findViewById(R.id.download_button)
        progressBar = findViewById(R.id.progress_bar)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        
        val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "Unknown Book"
        val bookAuthors = intent.getStringExtra(EXTRA_BOOK_AUTHORS) ?: "Unknown Author"
        allUuids = intent.getStringArrayListExtra(EXTRA_ALL_UUIDS)
        
        toolbar.title = bookTitle
        bookTitleText.text = buildString {
            append(bookTitle)
            append("\nby ")
            append(bookAuthors)
        }
        
        downloadButton.isEnabled = false
        downloadButton.setOnClickListener {
            downloadAnnotationsAsJson()
        }
        
        loadAnnotations(allUuids)
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        // Ensure refresh icon is black
        menu.findItem(R.id.action_refresh)?.icon?.setTint(android.graphics.Color.BLACK)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadAnnotations(allUuids)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun loadAnnotations(allUuids: List<String>?) {
        progressBar.visibility = View.VISIBLE
        annotationCountText.text = "Loading annotations..."
        
        lifecycleScope.launch {
            annotations = withContext(Dispatchers.IO) {
                OnyxContentProvider.queryAnnotations(this@BookDetailActivity, allUuids)
            }
            
            progressBar.visibility = View.GONE
            
            val count = annotations.size
            annotationCountText.text = when (count) {
                0 -> "No annotations found"
                1 -> "1 annotation found"
                else -> "$count annotations found"
            }
            
            downloadButton.isEnabled = count > 0
        }
    }
    
    private fun downloadAnnotationsAsJson() {
        if (annotations.isEmpty()) {
            Toast.makeText(this, "No annotations to download", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    // Sort annotations by page number first, then by creation time
                    val sortedAnnotations = annotations.sortedWith(
                        compareBy({ it.pageNumber }, { it.createdAt })
                    )
                    
                    // Build annotations array
                    val annotationsArray = JSONArray()
                    sortedAnnotations.forEach { annotation ->
                        val entry = JSONObject().apply {
                            // Add the quote text
                            annotation.quote?.let { put("quote", it) }
                            
                            // Add page number
                            annotation.pageNumber?.let { put("pageNumber", it) }
                            
                            // Add chapter
                            annotation.chapter?.let { put("chapter", it) }
                            
                            // Add timestamp in milliseconds
                            annotation.createdAt?.let { put("createdAt", it) }
                            
                            // Convert color integer to hex format
                            annotation.color?.let { colorInt ->
                                // Convert signed int to unsigned (Kotlin uses Long for unsigned operations)
                                val num = colorInt.toLong() and 0xFFFFFFFFL
                                val b = (num and 0xFF).toInt()
                                val g = ((num and 0xFF00) shr 8).toInt()
                                val r = ((num and 0xFF0000) shr 16).toInt()
                                val hexColor = "#%02x%02x%02x".format(r, g, b)
                                put("color", hexColor)
                            }
                            
                            // Map shape to style name
                            annotation.shape?.let { shape ->
                                val styleName = when (shape) {
                                    0 -> "highlight"
                                    1 -> "underline"
                                    2 -> "dashed"
                                    3 -> "wavy"
                                    4 -> "redact"
                                    5 -> "mute"
                                    else -> "unknown"
                                }
                                put("style", styleName)
                            }
                            
                            // Add note if exists
                            annotation.note?.let { 
                                if (it.isNotBlank()) put("note", it)
                            }
                        }
                        annotationsArray.put(entry)
                    }
                    
                    // Extract format from file path
                    val filePath = intent.getStringExtra(EXTRA_BOOK_FILE_PATH) ?: ""
                    val format = when {
                        filePath.endsWith(".epub", ignoreCase = true) -> "epub"
                        filePath.endsWith(".mobi", ignoreCase = true) -> "mobi"
                        filePath.endsWith(".azw3", ignoreCase = true) -> "azw3"
                        filePath.endsWith(".azw", ignoreCase = true) -> "azw"
                        else -> "unknown"
                    }
                    
                    // Build top-level JSON object
                    val rootObject = JSONObject().apply {
                        put("title", intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "Unknown")
                        put("authors", intent.getStringExtra(EXTRA_BOOK_AUTHORS) ?: "Unknown")
                        put("format", format)
                        put("annotations", annotationsArray)
                    }
                    
                    // Use MediaStore API for Android 10+
                    val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE)?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "book"
                    val fileName = "${bookTitle}_annotations_${System.currentTimeMillis()}.json"
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        // Android 10+ - Use MediaStore
                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                        }
                        
                        val uri = contentResolver.insert(
                            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            contentValues
                        )
                        
                        uri?.let {
                            contentResolver.openOutputStream(it)?.use { outputStream ->
                                outputStream.write(rootObject.toString(2).toByteArray())
                            }
                            fileName // Return filename for success message
                        }
                    } else {
                        // Android 9 and below - Direct file access
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val file = File(downloadsDir, fileName)
                        file.writeText(rootObject.toString(2))
                        fileName
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BookDetailActivity", "Error saving file", e)
                    null
                }
            }
            
            if (result != null) {
                Toast.makeText(this@BookDetailActivity, "Saved: $result", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@BookDetailActivity, "Failed to save annotations", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
