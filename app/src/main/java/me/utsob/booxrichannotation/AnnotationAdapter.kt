package me.utsob.booxrichannotation

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnnotationAdapter(
    private val annotations: List<Annotation>,
    private val onSelectionChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<AnnotationAdapter.AnnotationViewHolder>() {

    private val selectedPositions = mutableSetOf<Int>()
    private val timestampFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

    class AnnotationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.annotation_checkbox)
        val styleChip: TextView = view.findViewById(R.id.annotation_style_chip)
        val page: TextView = view.findViewById(R.id.annotation_page)
        val timestamp: TextView = view.findViewById(R.id.annotation_timestamp)
        val chapter: TextView = view.findViewById(R.id.annotation_chapter)
        val quote: TextView = view.findViewById(R.id.annotation_quote)
        val noteDivider: View = view.findViewById(R.id.annotation_note_divider)
        val note: TextView = view.findViewById(R.id.annotation_note)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnotationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_annotation, parent, false)
        return AnnotationViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnnotationViewHolder, position: Int) {
        val annotation = annotations[position]

        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = position in selectedPositions
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedPositions.add(position) else selectedPositions.remove(position)
            onSelectionChanged?.invoke()
        }

        holder.styleChip.text = annotation.styleName().replaceFirstChar { it.uppercase() }
        applyChipColor(holder.styleChip, annotation.colorHex())

        holder.page.text = annotation.pageNumber?.let { "Page $it" } ?: "Page unknown"

        holder.chapter.text = annotation.displayChapter() ?: ""

        holder.timestamp.text = annotation.createdAt?.let { timestampFormat.format(Date(it)) } ?: ""

        holder.quote.text = annotation.quote?.takeIf { it.isNotBlank() } ?: "(No highlighted text)"

        val note = annotation.displayNote()
        if (note != null) {
            holder.note.text = "Note: $note"
            holder.note.visibility = View.VISIBLE
            holder.noteDivider.visibility = View.VISIBLE
        } else {
            holder.note.visibility = View.GONE
            holder.noteDivider.visibility = View.GONE
        }
    }

    /** Fills the style chip with the annotation's actual highlight color, picking readable text contrast. */
    private fun applyChipColor(chip: TextView, colorHex: String?) {
        val colorInt = colorHex?.let {
            try {
                Color.parseColor(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        } ?: Color.WHITE

        (chip.background.mutate() as? GradientDrawable)?.setColor(colorInt)

        val luminance = 0.299 * Color.red(colorInt) + 0.587 * Color.green(colorInt) + 0.114 * Color.blue(colorInt)
        chip.setTextColor(if (luminance > 150) Color.BLACK else Color.WHITE)
    }

    fun selectAll() {
        selectedPositions.clear()
        selectedPositions.addAll(annotations.indices)
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    fun clearSelection() {
        selectedPositions.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    fun isAllSelected(): Boolean = annotations.isNotEmpty() && selectedPositions.size == annotations.size

    fun selectedCount(): Int = selectedPositions.size

    fun getSelectedAnnotations(): List<Annotation> = annotations.filterIndexed { index, _ -> index in selectedPositions }

    override fun getItemCount() = annotations.size
}
