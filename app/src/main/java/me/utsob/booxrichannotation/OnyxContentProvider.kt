package me.utsob.booxrichannotation

import android.content.Context
import android.net.Uri
import android.util.Log

object OnyxContentProvider {
    private const val TAG = "OnyxContentProvider"
    private const val METADATA_URI = "content://com.onyx.content.database.ContentProvider/Metadata"
    private const val ANNOTATION_URI = "content://com.onyx.content.database.ContentProvider/Annotation"
    
    fun queryBookMetadata(context: Context): List<BookMetadata> {
        val books = mutableListOf<BookMetadata>()
        val uri = Uri.parse(METADATA_URI)
        
        Log.d(TAG, "Starting query to $METADATA_URI")
        
        try {
            context.contentResolver.query(
                uri,
                null, // projection (null = all columns)
                null, // selection
                null, // selectionArgs
                null  // sortOrder
            )?.use { cursor ->
                Log.d(TAG, "Query successful! Found ${cursor.count} books")
                
                // Log all column names for debugging
                val columnNames = cursor.columnNames
                Log.d(TAG, "Available columns (${columnNames.size}): ${columnNames.joinToString(", ")}")
                
                var successCount = 0
                var errorCount = 0
                
                while (cursor.moveToNext()) {
                    try {
                        // Helper function to safely get string value
                        fun getStringOrNull(columnName: String): String? {
                            val index = cursor.getColumnIndex(columnName)
                            return if (index >= 0) {
                                val value = cursor.getString(index)
                                if (value.isNullOrBlank() || value == "NULL") null else value
                            } else {
                                Log.w(TAG, "Column '$columnName' not found")
                                null
                            }
                        }
                        
                        // Helper function to safely get long value
                        fun getLongOrNull(columnName: String): Long? {
                            val index = cursor.getColumnIndex(columnName)
                            return if (index >= 0) {
                                try {
                                    cursor.getLong(index)
                                } catch (e: Exception) {
                                    null
                                }
                            } else {
                                null
                            }
                        }
                        
                        val uuid = getStringOrNull("uuid") ?: continue
                        
                        val book = BookMetadata(
                            uuid = uuid,
                            title = getStringOrNull("title"),
                            name = getStringOrNull("name"),
                            authors = getStringOrNull("authors"),
                            publisher = getStringOrNull("publisher"),
                            language = getStringOrNull("language"),
                            isbn = getStringOrNull("ISBN"),
                            description = getStringOrNull("description"),
                            location = getStringOrNull("location"),
                            idString = getStringOrNull("idString"),
                            lastAccess = getLongOrNull("lastAccess")
                        )
                        
                        books.add(book)
                        successCount++
                        
                        // Log first few books for debugging
                        if (successCount <= 3) {
                            Log.d(TAG, "Book #$successCount: ${book.getDisplayTitle()} by ${book.getDisplayAuthors()}")
                        }
                    } catch (e: Exception) {
                        errorCount++
                        Log.e(TAG, "Error parsing book row #${cursor.position}", e)
                    }
                }
                
                Log.d(TAG, "Parsed $successCount books successfully, $errorCount errors")
            } ?: run {
                Log.e(TAG, "Query returned null cursor")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to access content provider", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying content provider", e)
        }
        
        Log.d(TAG, "Returning ${books.size} books")
        return books
    }
    
    fun queryAnnotations(context: Context, bookUuids: List<String>?): List<Annotation> {
        val annotations = mutableListOf<Annotation>()
        if (bookUuids.isNullOrEmpty()) {
            Log.w(TAG, "Cannot query annotations: bookUuids is null or empty")
            return annotations
        }
        
        val uri = Uri.parse(ANNOTATION_URI)
        
        Log.d(TAG, "Starting annotation query for ${bookUuids.size} UUIDs: ${bookUuids.take(3)}...")
        
        try {
            // Query annotations for all UUIDs
            // Build selection: "idString=? OR idString=? OR ..."
            val selection = bookUuids.joinToString(" OR ") { "idString=?" }
            
            context.contentResolver.query(
                uri,
                null,
                selection,
                bookUuids.toTypedArray(),
                null
            )?.use { cursor ->
                Log.d(TAG, "Annotation query successful! Found ${cursor.count} annotations")
                
                val columnNames = cursor.columnNames
                Log.d(TAG, "Available annotation columns: ${columnNames.joinToString(", ")}")
                
                var rowNumber = 0
                
                while (cursor.moveToNext()) {
                    try {
                        fun getStringOrNull(columnName: String): String? {
                            val index = cursor.getColumnIndex(columnName)
                            return if (index >= 0) {
                                val value = cursor.getString(index)
                                if (value.isNullOrBlank() || value == "NULL") null else value
                            } else null
                        }
                        
                        fun getIntOrNull(columnName: String): Int? {
                            val index = cursor.getColumnIndex(columnName)
                            return if (index >= 0 && !cursor.isNull(index)) {
                                cursor.getInt(index)
                            } else null
                        }
                        
                        fun getLongOrNull(columnName: String): Long? {
                            val index = cursor.getColumnIndex(columnName)
                            return if (index >= 0 && !cursor.isNull(index)) {
                                cursor.getLong(index)
                            } else null
                        }
                        
                        val annotation = Annotation(
                            rowNumber = rowNumber++,
                            quote = getStringOrNull("quote"),
                            locationBegin = getStringOrNull("locationBegin"),
                            locationEnd = getStringOrNull("locationEnd"),
                            locationBeginInt = getIntOrNull("locationBeginInt"),
                            locationEndInt = getIntOrNull("locationEndInt"),
                            note = getStringOrNull("note"),
                            linkNote = getStringOrNull("linkNote"),
                            application = getStringOrNull("application"),
                            position = getStringOrNull("position"),
                            pageNumber = getIntOrNull("pageNumber"),
                            rectangles = getStringOrNull("rectangles"),
                            color = getIntOrNull("color"),
                            shape = getIntOrNull("shape"),
                            chapter = getStringOrNull("chapter"),
                            uuid = getStringOrNull("uuid"),
                            objId = getStringOrNull("objId"),
                            status = getIntOrNull("status"),
                            pageXpath = getStringOrNull("pageXpath"),
                            startXpath = getStringOrNull("startXpath"),
                            endXpath = getStringOrNull("endXpath"),
                            customAttr = getStringOrNull("customAttr"),
                            id = getIntOrNull("id"),
                            guid = getStringOrNull("guid"),
                            idString = getStringOrNull("idString"),
                            createdAt = getLongOrNull("createdAt"),
                            updatedAt = getLongOrNull("updatedAt")
                        )
                        
                        annotations.add(annotation)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing annotation row", e)
                    }
                }
                
                Log.d(TAG, "Parsed ${annotations.size} annotations successfully")
                
                // Deduplicate annotations - group by a composite key (quote + locationBeginInt)
                // and keep only the most recent version (highest updatedAt)
                val deduplicatedAnnotations = annotations
                    .groupBy { ann ->
                        // Create a unique key from quote and location
                        "${ann.quote?.take(100)}_${ann.locationBeginInt}_${ann.locationEndInt}"
                    }
                    .mapNotNull { (key, annotationsWithSameKey) ->
                        // Keep the one with the latest updatedAt
                        annotationsWithSameKey.maxByOrNull { it.updatedAt ?: 0L }
                    }
                
                Log.d(TAG, "After deduplication: ${deduplicatedAnnotations.size} annotations (removed ${annotations.size - deduplicatedAnnotations.size} duplicates)")
                
                return deduplicatedAnnotations
            } ?: run {
                Log.e(TAG, "Annotation query returned null cursor")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying annotations", e)
        }
        
        return annotations
    }
    
    fun queryAllAnnotations(context: Context): List<Annotation> {
        val annotations = mutableListOf<Annotation>()
        val uri = Uri.parse(ANNOTATION_URI)
        
        Log.d(TAG, "Querying ALL annotations from content provider...")
        
        try {
            context.contentResolver.query(
                uri,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                Log.d(TAG, "Found ${cursor.count} total annotations")
                
                while (cursor.moveToNext()) {
                    try {
                        fun getStringOrNull(columnName: String): String? {
                            val index = cursor.getColumnIndex(columnName)
                            return if (index >= 0) {
                                val value = cursor.getString(index)
                                if (value.isNullOrBlank() || value == "NULL") null else value
                            } else null
                        }
                        
                        fun getIntOrNull(columnName: String): Int? {
                            val index = cursor.getColumnIndex(columnName)
                            return if (index >= 0) {
                                try { cursor.getInt(index) } catch (e: Exception) { null }
                            } else null
                        }
                        
                        fun getLongOrNull(columnName: String): Long? {
                            val index = cursor.getColumnIndex(columnName)
                            return if (index >= 0) {
                                try { cursor.getLong(index) } catch (e: Exception) { null }
                            } else null
                        }
                        
                        val annotation = Annotation(
                            rowNumber = cursor.position,
                            quote = getStringOrNull("quote"),
                            locationBegin = getStringOrNull("locationBegin"),
                            locationEnd = getStringOrNull("locationEnd"),
                            locationBeginInt = getIntOrNull("locationBeginInt"),
                            locationEndInt = getIntOrNull("locationEndInt"),
                            note = getStringOrNull("note"),
                            linkNote = getStringOrNull("linkNote"),
                            application = getStringOrNull("application"),
                            position = getStringOrNull("position"),
                            pageNumber = getIntOrNull("pageNumber"),
                            rectangles = getStringOrNull("rectangles"),
                            color = getIntOrNull("color"),
                            shape = getIntOrNull("shape"),
                            chapter = getStringOrNull("chapter"),
                            uuid = getStringOrNull("uuid"),
                            objId = getStringOrNull("objId"),
                            status = getIntOrNull("status"),
                            pageXpath = getStringOrNull("pageXpath"),
                            startXpath = getStringOrNull("startXpath"),
                            endXpath = getStringOrNull("endXpath"),
                            customAttr = getStringOrNull("customAttr"),
                            id = getIntOrNull("id"),
                            guid = getStringOrNull("guid"),
                            idString = getStringOrNull("idString"),
                            createdAt = getLongOrNull("createdAt"),
                            updatedAt = getLongOrNull("updatedAt")
                        )
                        
                        annotations.add(annotation)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing annotation row #${cursor.position}", e)
                    }
                }
                
                // Deduplicate annotations using composite key
                val deduplicatedAnnotations = annotations
                    .groupBy {
                        val quoteKey = it.quote?.take(100) ?: ""
                        val locBegin = it.locationBeginInt ?: 0
                        val locEnd = it.locationEndInt ?: 0
                        "${quoteKey}_${locBegin}_${locEnd}"
                    }
                    .mapNotNull { (key, annotationsWithSameKey) ->
                        annotationsWithSameKey.maxByOrNull { it.updatedAt ?: 0L }
                    }
                
                Log.d(TAG, "After deduplication: ${deduplicatedAnnotations.size} annotations (removed ${annotations.size - deduplicatedAnnotations.size} duplicates)")
                return deduplicatedAnnotations
            } ?: run {
                Log.e(TAG, "Query returned null cursor")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying all annotations", e)
        }
        
        return annotations
    }
}
