package me.utsob.booxrichannotation

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
    
    private var allBooksWithAnnotations: List<BookWithAnnotations> = emptyList()
    private var filteredBooksWithAnnotations: List<BookWithAnnotations> = emptyList()
    
    // Sorting preference
    private enum class SortMode {
        LAST_READ, ALPHABETICAL
    }
    private var currentSortMode = SortMode.LAST_READ
    
    companion object {
        private const val PREFS_NAME = "BooxRichAnnotationPrefs"
        private const val KEY_SORT_MODE = "sort_mode"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load saved sort preference
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentSortMode = SortMode.valueOf(
            prefs.getString(KEY_SORT_MODE, SortMode.LAST_READ.name) ?: SortMode.LAST_READ.name
        )
        
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
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                showSortMenu(item)
                true
            }
            R.id.action_refresh -> {
                loadBooks()
                true
            }
            R.id.action_preferences -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showSortMenu(menuItem: MenuItem) {
        val popup = android.widget.PopupMenu(this, toolbar.findViewById(R.id.action_sort))
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)
        
        // Check current sort mode
        when (currentSortMode) {
            SortMode.LAST_READ -> popup.menu.findItem(R.id.sort_last_read)?.isChecked = true
            SortMode.ALPHABETICAL -> popup.menu.findItem(R.id.sort_alphabetical)?.isChecked = true
        }
        
        popup.setOnMenuItemClickListener { item ->
            val newSortMode = when (item.itemId) {
                R.id.sort_last_read -> SortMode.LAST_READ
                R.id.sort_alphabetical -> SortMode.ALPHABETICAL
                else -> return@setOnMenuItemClickListener false
            }
            
            if (newSortMode != currentSortMode) {
                currentSortMode = newSortMode
                item.isChecked = true
                
                // Save preference
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SORT_MODE, currentSortMode.name)
                    .apply()
                
                // Resort and update display
                sortAndFilterBooks()
            }
            true
        }
        
        popup.show()
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
        
        filteredBooksWithAnnotations = if (searchQuery.isEmpty()) {
            allBooksWithAnnotations
        } else {
            allBooksWithAnnotations.filter { bookWithAnnotations ->
                val title = bookWithAnnotations.book.getDisplayTitle().lowercase()
                val author = bookWithAnnotations.book.getDisplayAuthors().lowercase()
                title.contains(searchQuery) || author.contains(searchQuery)
            }
        }
        
        updateRecyclerView()
    }
    
    private fun sortAndFilterBooks() {
        android.util.Log.d("MainActivity", "sortAndFilterBooks: currentSortMode=$currentSortMode, allBooksWithAnnotations.size=${allBooksWithAnnotations.size}")
        
        // Sort all books first
        allBooksWithAnnotations = when (currentSortMode) {
            SortMode.LAST_READ -> {
                android.util.Log.d("MainActivity", "Sorting by LAST_READ")
                // Sort by lastAccess descending (most recent first), null values last
                allBooksWithAnnotations.sortedWith(
                    compareByDescending<BookWithAnnotations> { it.book.lastAccess != null }
                        .thenByDescending { it.book.lastAccess ?: 0 }
                )
            }
            SortMode.ALPHABETICAL -> {
                android.util.Log.d("MainActivity", "Sorting by ALPHABETICAL")
                // Sort alphabetically by display title
                allBooksWithAnnotations.sortedBy { it.book.getDisplayTitle().lowercase() }
            }
        }
        
        android.util.Log.d("MainActivity", "After sorting, first 3 books:")
        allBooksWithAnnotations.take(3).forEachIndexed { i, bookWithAnnotations ->
            android.util.Log.d("MainActivity", "  #$i: ${bookWithAnnotations.book.getDisplayTitle()}")
        }
        
        // Reapply current search filter
        filterBooks(searchView.query.toString())
    }
    
    private fun updateRecyclerView() {
        if (filteredBooksWithAnnotations.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
            emptyText.text = if (searchView.query.isEmpty()) {
                "No books with annotations found"
            } else {
                "No books found matching \"${searchView.query}\""
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            recyclerView.adapter = BookAdapter(filteredBooksWithAnnotations, lifecycleScope)
        }
    }
    
    private fun loadBooks() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyText.visibility = View.GONE
        
        lifecycleScope.launch {
            val (books, allAnnotations) = withContext(Dispatchers.IO) {
                val books = OnyxContentProvider.queryBookMetadata(this@MainActivity)
                val annotations = OnyxContentProvider.queryAllAnnotations(this@MainActivity)
                Pair(books, annotations)
            }
            
            // Group by idString and collect all UUIDs for each file
            val uniqueBooks = books.groupBy { it.idString }
                .map { (_, booksWithSameFile) ->
                    val firstBook = booksWithSameFile.first()
                    // Collect all UUIDs for this file and keep the most recent lastAccess time
                    val allUuids = booksWithSameFile.map { it.uuid }
                    val latestAccessTime = booksWithSameFile.mapNotNull { it.lastAccess }.maxOrNull()
                    firstBook.copy(allUuids = allUuids, lastAccess = latestAccessTime)
                }
            
            // Map annotations to books by idString
            val annotationsByIdString = allAnnotations.groupBy { it.idString }
            
            android.util.Log.d("MainActivity", "Total unique books loaded: ${uniqueBooks.size}")
            android.util.Log.d("MainActivity", "Total annotations loaded: ${allAnnotations.size}")
            android.util.Log.d("MainActivity", "Unique annotation idStrings: ${annotationsByIdString.keys.size}")
            
            // Create BookWithAnnotations list and filter to only books with annotations
            allBooksWithAnnotations = uniqueBooks.mapNotNull { book ->
                // Find annotations that match any of this book's UUIDs
                val bookAnnotations = book.allUuids.flatMap { uuid ->
                    annotationsByIdString[uuid] ?: emptyList()
                }.distinctBy { "${it.quote?.take(100)}_${it.locationBeginInt}_${it.locationEndInt}" }
                
                // Only include books with at least 1 annotation
                if (bookAnnotations.isNotEmpty()) {
                    BookWithAnnotations(book, bookAnnotations)
                } else {
                    // Log books that have no annotations matched
                    if (book.getDisplayTitle().contains("godel", ignoreCase = true) || 
                        book.getDisplayTitle().contains("escher", ignoreCase = true)) {
                        android.util.Log.w("MainActivity", "Book '${book.getDisplayTitle()}' has 0 annotations matched. UUIDs: ${book.allUuids}")
                    }
                    null
                }
            }
            
            android.util.Log.d("MainActivity", "Loaded ${allBooksWithAnnotations.size} books with annotations (filtered out books with 0 annotations)")
            android.util.Log.d("MainActivity", "Total annotations across all books: ${allBooksWithAnnotations.sumOf { it.annotationCount }}")
            
            // Apply sorting based on current mode
            sortAndFilterBooks()
            
            progressBar.visibility = View.GONE
        }
    }
}