package me.utsob.booxrichannotation

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookAdapter(private val books: List<BookMetadata>) : 
    RecyclerView.Adapter<BookAdapter.BookViewHolder>() {
    
    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.book_title)
        val authorTextView: TextView = view.findViewById(R.id.book_author)
        val detailsTextView: TextView = view.findViewById(R.id.book_details)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.titleTextView.text = book.getDisplayTitle()
        holder.authorTextView.text = book.getDisplayAuthors()
        
        // Build details string
        val details = buildList {
            book.publisher?.let { if (it != "NULL") add("Publisher: $it") }
            book.language?.let { if (it != "NULL") add("Language: $it") }
            book.isbn?.let { if (it != "NULL") add("ISBN: $it") }
        }.joinToString(" • ")
        
        holder.detailsTextView.text = details.ifBlank { "No additional details" }
        holder.detailsTextView.visibility = if (details.isNotBlank()) View.VISIBLE else View.GONE
        
        // Set click listener to open detail activity
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, BookDetailActivity::class.java).apply {
                putStringArrayListExtra(BookDetailActivity.EXTRA_ALL_UUIDS, ArrayList(book.allUuids))
                putExtra(BookDetailActivity.EXTRA_BOOK_TITLE, book.getDisplayTitle())
                putExtra(BookDetailActivity.EXTRA_BOOK_AUTHORS, book.getDisplayAuthors())
                putExtra(BookDetailActivity.EXTRA_BOOK_FILE_PATH, book.idString)
            }
            context.startActivity(intent)
        }
    }
    
    override fun getItemCount() = books.size
}
