package me.utsob.booxrichannotation

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class PreferencesActivity : AppCompatActivity() {
    
    private lateinit var formatRadioGroup: RadioGroup
    private lateinit var jsonRadio: RadioButton
    private lateinit var csvRadio: RadioButton
    private lateinit var textRadio: RadioButton
    private lateinit var textExtensionInput: EditText
    private lateinit var btnEditTemplate: Button
    
    companion object {
        const val PREFS_NAME = "BooxRichAnnotationPrefs"
        const val KEY_EXPORT_FORMAT = "export_format"
        const val KEY_TEXT_EXTENSION = "text_extension"
        const val KEY_TEXT_TEMPLATE = "text_template"
        const val FORMAT_JSON = "json"
        const val FORMAT_CSV = "csv"
        const val FORMAT_TEXT = "text"
        const val DEFAULT_TEXT_EXTENSION = "md"
        const val DEFAULT_TEMPLATE = """# {{ book.title }}
##### by {{ book.authors }}

{%- set prevChapter = "" %}
{%- for annotation in annotations %}
{%- if annotation.chapter != prevChapter %}

## {{ annotation.chapter }}
{%- set prevChapter = annotation.chapter %}
{%- endif %}
### Page: {{ annotation.pageNumber }} @ {{ annotation.createdAt | date("yyyy-MM-dd HH:mm:ss") }}
{{ annotation.quote }}

---
{{ annotation.note }}

**Color**: {{ annotation.color }}
**Style**: {{ annotation.style }}
{% endfor %}

---

| Metadata | Value |
|----------|-------|
| Total Pages | {{ book.totalPages }} |
| Annotations | {{ annotations.size }} |
| Exported At | {{ book.exportedAt | date("yyyy-MM-dd HH:mm:ss") }} |
"""
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        
        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Preferences"
        
        formatRadioGroup = findViewById(R.id.format_radio_group)
        jsonRadio = findViewById(R.id.radio_json)
        csvRadio = findViewById(R.id.radio_csv)
        textRadio = findViewById(R.id.radio_text)
        textExtensionInput = findViewById(R.id.text_extension_input)
        btnEditTemplate = findViewById(R.id.btn_edit_template)
        
        // Style the button programmatically
        btnEditTemplate.apply {
            backgroundTintList = null
            setBackgroundResource(R.drawable.bg_dialog_button)
            setPadding(48, 48, 48, 48)
        }
        
        // Load saved preferences
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentFormat = prefs.getString(KEY_EXPORT_FORMAT, FORMAT_JSON) ?: FORMAT_JSON
        val textExtension = prefs.getString(KEY_TEXT_EXTENSION, DEFAULT_TEXT_EXTENSION) ?: DEFAULT_TEXT_EXTENSION
        
        when (currentFormat) {
            FORMAT_JSON -> jsonRadio.isChecked = true
            FORMAT_CSV -> csvRadio.isChecked = true
            FORMAT_TEXT -> textRadio.isChecked = true
        }
        
        textExtensionInput.setText(textExtension)
        
        // Save preference when changed
        formatRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val format = when (checkedId) {
                R.id.radio_json -> FORMAT_JSON
                R.id.radio_csv -> FORMAT_CSV
                R.id.radio_text -> FORMAT_TEXT
                else -> FORMAT_JSON
            }
            
            prefs.edit().putString(KEY_EXPORT_FORMAT, format).apply()
        }
        
        // Save extension when changed
        textExtensionInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val extension = textExtensionInput.text.toString().trim()
                if (extension.isNotEmpty()) {
                    prefs.edit().putString(KEY_TEXT_EXTENSION, extension).apply()
                }
            }
        }
        
        // Open template editor
        btnEditTemplate.setOnClickListener {
            startActivity(Intent(this, TemplateEditorActivity::class.java))
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
