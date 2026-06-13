package me.utsob.booxrichannotation

data class BookMetadata(
    val uuid: String,
    val title: String? = null,
    val name: String? = null,
    val authors: String? = null,
    val publisher: String? = null,
    val language: String? = null,
    val isbn: String? = null,
    val description: String? = null,
    val location: String? = null,
    val idString: String? = null,
    val lastAccess: Long? = null,
    val allUuids: List<String> = listOf(uuid) // Track all UUIDs for this file
) {
    fun getDisplayTitle(): String {
        val cleanedName = name?.let { n ->
            var cleaned = n
            val extensions = listOf(".epub", ".pdf", ".txt", ".cbz", ".cbr", ".mobi", ".azw3")
            extensions.forEach { ext ->
                if (cleaned.endsWith(ext, ignoreCase = true)) {
                    cleaned = cleaned.substring(0, cleaned.length - ext.length)
                }
            }
            cleaned
        }
        
        return when {
            !title.isNullOrBlank() && title != "NULL" -> title
            !cleanedName.isNullOrBlank() && cleanedName != "NULL" -> cleanedName
            else -> "Unknown Book ($uuid)"
        }
    }
    
    fun getDisplayAuthors(): String {
        return if (!authors.isNullOrBlank() && authors != "NULL") authors else "Unknown Author"
    }
}
