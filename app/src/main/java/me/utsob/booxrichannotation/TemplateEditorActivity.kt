package me.utsob.booxrichannotation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter

class TemplateEditorActivity : AppCompatActivity() {
    
    private lateinit var templateInput: EditText
    private lateinit var errorText: TextView
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private val pebbleEngine = PebbleEngine.Builder()
        .strictVariables(false)
        .autoEscaping(false)
        .newLineTrimming(false)
        .extension(CustomPebbleExtension())
        .build()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_editor)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Template Editor"
        
        templateInput = findViewById(R.id.template_input)
        errorText = findViewById(R.id.error_text)
        btnSave = findViewById(R.id.btn_save)
        btnReset = findViewById(R.id.btn_reset)
        
        // Style the buttons programmatically
        btnSave.apply {
            backgroundTintList = null
            setBackgroundResource(R.drawable.bg_dialog_button)
            setPadding(48, 48, 48, 48)
        }
        btnReset.apply {
            backgroundTintList = null
            setBackgroundResource(R.drawable.bg_dialog_button)
            setPadding(48, 48, 48, 48)
        }
        
        // Load saved template
        val prefs = getSharedPreferences(PreferencesActivity.PREFS_NAME, MODE_PRIVATE)
        val template = prefs.getString(
            PreferencesActivity.KEY_TEXT_TEMPLATE,
            PreferencesActivity.DEFAULT_TEMPLATE
        ) ?: PreferencesActivity.DEFAULT_TEMPLATE
        
        templateInput.setText(template)
        
        // Add text watcher for linting
        templateInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateTemplate(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        // Initial validation
        validateTemplate(template)
        
        // Save button
        btnSave.setOnClickListener {
            val newTemplate = templateInput.text.toString()
            if (validateTemplate(newTemplate)) {
                prefs.edit().putString(PreferencesActivity.KEY_TEXT_TEMPLATE, newTemplate).apply()
                finish()
            }
        }
        
        // Reset button
        btnReset.setOnClickListener {
            templateInput.setText(PreferencesActivity.DEFAULT_TEMPLATE)
        }
    }
    
    private fun validateTemplate(template: String): Boolean {
        return try {
            // Test compile the template
            pebbleEngine.getLiteralTemplate(template)
            
            // Try rendering with sample data
            val compiledTemplate = pebbleEngine.getLiteralTemplate(template)
            val context = mapOf(
                "book" to mapOf(
                    "title" to "Sample Book",
                    "authors" to "John Doe",
                    "format" to "epub",
                    "totalPages" to 300,
                    "exportedAt" to System.currentTimeMillis()
                ),
                "annotations" to listOf(
                    mapOf(
                        "pageNumber" to 42,
                        "quote" to "Sample quote from the book.",
                        "note" to "This is a sample note.",
                        "chapter" to "Chapter 1",
                        "style" to "highlight",
                        "color" to "#ffff00",
                        "createdAt" to System.currentTimeMillis()
                    )
                )
            )
            compiledTemplate.evaluate(StringWriter(), context)
            
            errorText.text = "✓ Template is valid"
            errorText.setTextColor(android.graphics.Color.parseColor("#000000"))
            btnSave.isEnabled = true
            true
        } catch (e: Exception) {
            // Truncate error message to avoid showing entire template
            val errorMsg = e.message?.take(200) ?: "Unknown error"
            errorText.text = "✗ Error: $errorMsg"
            errorText.setTextColor(android.graphics.Color.parseColor("#000000"))
            btnSave.isEnabled = false
            false
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
