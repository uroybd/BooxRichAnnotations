package me.utsob.booxrichannotation

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var searchView: SearchView
    private lateinit var toolbar: MaterialToolbar
    
    private var allBooks: List<BookMetadata> = emptyList()
    private var filteredBooks: List<BookMetadata> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Disable animations for e-ink
        window.setWindowAnimations(0)
        overridePendingTransition(0, 0)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        recyclerView = findViewById(R.id.books_recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        emptyText = findViewById(R.id.empty_text)
        searchView = findViewById(R.id.search_view)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        setupSearch()
        loadBooks()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        // Ensure refresh icon is black
        menu.findItem(R.id.action_refresh)?.icon?.setTint(android.graphics.Color.BLACK)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadBooks()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupSearch() {
        // Set hint text color to black for better contrast
        val searchText = searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchText?.setHintTextColor(android.graphics.Color.parseColor("#666666"))
        searchText?.setTextColor(android.graphics.Color.BLACK)
        
        // Set icon tints to black
        val searchIcon = searchView.findViewById<android.widget.ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon?.setColorFilter(android.graphics.Color.BLACK)
        
        val closeIcon = searchView.findViewById<android.widget.ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon?.setColorFilter(android.graphics.Color.BLACK)
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                filterBooks(newText ?: "")
                return true
            }
        })
    }
    
    private fun filterBooks(query: String) {
        val searchQuery = query.trim().lowercase()
        
        filteredBooks = if (searchQuery.isEmpty()) {
            allBooks
        } else {
            allBooks.filter { book ->
                val title = book.getDisplayTitle().lowercase()
                val author = book.getDisplayAuthors().lowercase()
                title.contains(searchQuery) || author.contains(searchQuery)
            }
        }
        
        updateRecyclerView()
    }
    
    private fun updateRecyclerView() {
        if (filteredBooks.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
            emptyText.text = if (searchView.query.isEmpty()) {
                "No ebooks found (epub, mobi, azw/azw3)"
            } else {
                "No books found matching \"${searchView.query}\""
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            recyclerView.adapter = BookAdapter(filteredBooks)
        }
    }
    
    private fun loadBooks() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyText.visibility = View.GONE
        
        lifecycleScope.launch {
            val books = withContext(Dispatchers.IO) {
                OnyxContentProvider.queryBookMetadata(this@MainActivity)
            }
            
            // Filter for ebook formats (epub, mobi, azw, azw3)
            val ebookBooks = books.filter { book ->
                val fileName = book.name ?: book.location ?: ""
                fileName.endsWith(".epub", ignoreCase = true) ||
                fileName.endsWith(".mobi", ignoreCase = true) ||
                fileName.endsWith(".azw", ignoreCase = true) ||
                fileName.endsWith(".azw3", ignoreCase = true)
            }
            
            // Group by idString and collect all UUIDs for each file
            val uniqueBooks = ebookBooks.groupBy { it.idString }
                .map { (_, booksWithSameFile) ->
                    val firstBook = booksWithSameFile.first()
                    // Collect all UUIDs for this file
                    val allUuids = booksWithSameFile.map { it.uuid }
                    firstBook.copy(allUuids = allUuids)
                }
            
            // Sort alphabetically by display title
            allBooks = uniqueBooks.sortedBy { it.getDisplayTitle().lowercase() }
            filteredBooks = allBooks
            
            progressBar.visibility = View.GONE
            updateRecyclerView()
        }
    }
}