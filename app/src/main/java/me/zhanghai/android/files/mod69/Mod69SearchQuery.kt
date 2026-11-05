package me.zhanghai.android.files.mod69

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 69 mod: search words like MiXplorer. Mix them with normal text:
 *   type:image|video|audio|doc|archive|apk     ext:jpg,png
 *   >10mb  <500kb  (also kb, mb, gb)
 *   after:2026-01-31  before:2026-02-28  days:7 (changed in the last 7 days)
 *   dir: (only folders)  file: (only files)
 */
class Mod69SearchQuery(raw: String) {
    private var text = ""
    private var exts: Set<String>? = null
    private var minSize = -1L
    private var maxSize = -1L
    private var after = -1L
    private var before = -1L
    private var onlyDirs = false
    private var onlyFiles = false
    val hasFilters: Boolean

    companion object {
        val TYPES = mapOf(
            "image" to setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "avif", "svg"),
            "video" to setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "flv", "ts", "wmv"),
            "audio" to setOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "wma"),
            "doc" to setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "odt", "epub", "csv"),
            "archive" to setOf("zip", "rar", "7z", "tar", "gz", "xz", "bz2", "cbz", "cbr"),
            "apk" to setOf("apk", "apks", "xapk", "apkm"),
        )

        private fun size(v: String): Long? {
            val m = Regex("^(\\d+(?:\\.\\d+)?)(b|kb|k|mb|m|gb|g)?$").find(v.lowercase()) ?: return null
            val n = m.groupValues[1].toDouble()
            val mul = when (m.groupValues[2]) {
                "kb", "k" -> 1024.0
                "mb", "m" -> 1024.0 * 1024
                "gb", "g" -> 1024.0 * 1024 * 1024
                else -> 1.0
            }
            return (n * mul).toLong()
        }

        private fun date(v: String): Long? =
            runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(v)?.time }.getOrNull()
    }

    init {
        val rest = mutableListOf<String>()
        var any = false
        raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { w ->
            val lw = w.lowercase()
            when {
                lw.startsWith("type:") -> {
                    exts = (exts ?: emptySet()) + lw.removePrefix("type:").split(',', '|').flatMap { TYPES[it] ?: emptySet() }
                    any = true
                }
                lw.startsWith("ext:") -> {
                    exts = (exts ?: emptySet()) + lw.removePrefix("ext:").split(',', '|').map { it.trimStart('.') }
                    any = true
                }
                lw.startsWith(">") && size(lw.drop(1)) != null -> { minSize = size(lw.drop(1))!!; any = true }
                lw.startsWith("<") && size(lw.drop(1)) != null -> { maxSize = size(lw.drop(1))!!; any = true }
                lw.startsWith("after:") && date(lw.drop(6)) != null -> { after = date(lw.drop(6))!!; any = true }
                lw.startsWith("before:") && date(lw.drop(7)) != null -> { before = date(lw.drop(7))!! + 86_400_000L; any = true }
                lw.startsWith("days:") && lw.drop(5).toIntOrNull() != null -> {
                    after = System.currentTimeMillis() - lw.drop(5).toInt() * 86_400_000L; any = true
                }
                lw == "dir:" -> { onlyDirs = true; any = true }
                lw == "file:" -> { onlyFiles = true; any = true }
                else -> rest.add(w)
            }
        }
        text = rest.joinToString(" ")
        hasFilters = any
    }

    fun matches(name: String, isDir: Boolean, size: Long, modified: Long): Boolean {
        if (text.isNotEmpty() && !name.contains(text, ignoreCase = true)) return false
        if (!hasFilters) return true
        if (onlyDirs && !isDir) return false
        if (onlyFiles && isDir) return false
        exts?.let { set ->
            if (isDir) return false
            if (name.substringAfterLast('.', "").lowercase() !in set) return false
        }
        if (minSize >= 0 && (isDir || size < minSize)) return false
        if (maxSize >= 0 && (isDir || size > maxSize)) return false
        if (after >= 0 && modified < after) return false
        if (before >= 0 && modified >= before) return false
        return true
    }
}
