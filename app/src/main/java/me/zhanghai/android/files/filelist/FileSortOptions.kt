/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filelist

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.files.compat.reversedCompat
import me.zhanghai.android.files.file.FileItem

@Parcelize
data class FileSortOptions(
    val by: By,
    val order: Order,
    val isDirectoriesFirst: Boolean
) : Parcelable {
    fun createComparator(): Comparator<FileItem> {
        var comparator = compareBy<FileItem> {
            NAME_UNIMPORTANT_PREFIXES.any { prefix -> it.name.startsWith(prefix) }
        }.thenBy { it.nameCollationKey }
        when (by) {
            // Nothing to do.
            By.NAME -> {}
            By.TYPE ->
                comparator = compareBy<FileItem, String>(String.CASE_INSENSITIVE_ORDER) {
                    it.extension
                }.then(comparator)
            By.SIZE -> comparator = compareBy<FileItem> { it.attributes.size() }.then(comparator)
            By.LAST_MODIFIED ->
                comparator = compareBy<FileItem> { it.attributes.lastModifiedTime() }
                    .then(comparator)
        }
        // 69 mod: "Shuffled" (MiXplorer): a random order, new each time the app starts
        if (me.zhanghai.android.files.mod69.Mod69Look.shuffled) {
            val seed = me.zhanghai.android.files.mod69.Mod69Look.shuffleSeed
            comparator = compareBy<FileItem> { (it.name.hashCode() xor seed) * -0x61c88647 }.then(comparator)
        }
        when (order) {
            Order.ASCENDING -> {}
            Order.DESCENDING -> comparator = comparator.reversedCompat()
        }
        if (isDirectoriesFirst) {
            // 69 mod: "Files first" turns the grouping around
            val filesFirst = me.zhanghai.android.files.mod69.Mod69Look.filesFirst
            val isDirectoryComparator = compareBy<FileItem> { it.attributes.isDirectory }
                .let { if (filesFirst) it else it.reversedCompat() }
            comparator = isDirectoryComparator.then(comparator)
        }
        // 69 mod: "Dot files first"
        if (me.zhanghai.android.files.mod69.Mod69Look.dotFilesFirst) {
            comparator = compareBy<FileItem> { !it.name.startsWith(".") }.then(comparator)
        }
        return comparator
    }

    companion object {
        // Same behavior as Nautilus.
        private val NAME_UNIMPORTANT_PREFIXES = listOf(".", "#")
    }

    enum class By {
        NAME,
        TYPE,
        SIZE,
        LAST_MODIFIED
    }

    enum class Order {
        ASCENDING,
        DESCENDING
    }
}
