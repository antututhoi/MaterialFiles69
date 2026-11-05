package me.zhanghai.android.files.mod69

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

/**
 * 69 mod: lets other apps use folders picked in 69-Material Files (ACTION_OPEN_DOCUMENT_TREE).
 * Document ids are absolute file paths. Material Files has "All files access", so it can serve any folder.
 */
class Mod69DocumentsProvider : DocumentsProvider() {

    companion object {
        fun authority(packageName: String) = "$packageName.mod69documents"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_ICON, Root.COLUMN_AVAILABLE_BYTES,
        )
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE,
        )
    }

    override fun onCreate(): Boolean = true

    private fun fileFor(docId: String): File {
        val f = File(docId)
        if (!f.isAbsolute) throw FileNotFoundException("bad id $docId")
        return f
    }

    private fun mimeOf(f: File): String {
        if (f.isDirectory) return Document.MIME_TYPE_DIR
        val ext = f.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun addRow(c: MatrixCursor, f: File) {
        var flags = 0
        if (f.canWrite()) {
            flags = if (f.isDirectory) {
                flags or Document.FLAG_DIR_SUPPORTS_CREATE
            } else {
                flags or Document.FLAG_SUPPORTS_WRITE
            }
            flags = flags or Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_RENAME or
                Document.FLAG_SUPPORTS_MOVE or Document.FLAG_SUPPORTS_COPY
        }
        val mime = mimeOf(f)
        if (mime.startsWith("image/")) flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL
        c.newRow()
            .add(Document.COLUMN_DOCUMENT_ID, f.absolutePath)
            .add(Document.COLUMN_MIME_TYPE, mime)
            .add(Document.COLUMN_DISPLAY_NAME, f.name.ifEmpty { f.absolutePath })
            .add(Document.COLUMN_LAST_MODIFIED, f.lastModified())
            .add(Document.COLUMN_FLAGS, flags)
            .add(Document.COLUMN_SIZE, if (f.isFile) f.length() else null)
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val c = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val storage = Environment.getExternalStorageDirectory()
        c.newRow()
            .add(Root.COLUMN_ROOT_ID, "primary")
            .add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE or Root.FLAG_LOCAL_ONLY or Root.FLAG_SUPPORTS_IS_CHILD)
            .add(Root.COLUMN_TITLE, "69-Material Files")
            .add(Root.COLUMN_DOCUMENT_ID, storage.absolutePath)
            .add(Root.COLUMN_ICON, context!!.applicationInfo.icon)
            .add(Root.COLUMN_AVAILABLE_BYTES, storage.freeSpace)
        return c
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val c = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        addRow(c, fileFor(documentId))
        return c
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val c = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        fileFor(parentDocumentId).listFiles()?.forEach { addRow(c, it) }
        return c
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        val f = fileFor(documentId)
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode))
    }

    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        val parent = fileFor(parentDocumentId)
        val isDir = mimeType == Document.MIME_TYPE_DIR
        var name = displayName
        if (!isDir && !name.contains('.')) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.let { name = "$name.$it" }
        }
        var target = File(parent, name)
        var n = 1
        while (target.exists()) {
            val base = name.substringBeforeLast('.', name)
            val ext = name.substringAfterLast('.', "")
            target = File(parent, if (ext.isEmpty() || isDir) "$base ($n)" else "$base ($n).$ext")
            n++
        }
        val ok = if (isDir) target.mkdirs() else target.createNewFile()
        if (!ok) throw FileNotFoundException("cannot create $target")
        return target.absolutePath
    }

    override fun deleteDocument(documentId: String) {
        val f = fileFor(documentId)
        if (!(if (f.isDirectory) f.deleteRecursively() else f.delete())) throw FileNotFoundException("cannot delete")
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val f = fileFor(documentId)
        val target = File(f.parentFile, displayName)
        if (!f.renameTo(target)) throw FileNotFoundException("cannot rename")
        return target.absolutePath
    }

    override fun moveDocument(sourceDocumentId: String, sourceParentDocumentId: String, targetParentDocumentId: String): String {
        val f = fileFor(sourceDocumentId)
        val target = File(fileFor(targetParentDocumentId), f.name)
        if (!f.renameTo(target)) throw FileNotFoundException("cannot move")
        return target.absolutePath
    }

    override fun copyDocument(sourceDocumentId: String, targetParentDocumentId: String): String {
        val f = fileFor(sourceDocumentId)
        val target = File(fileFor(targetParentDocumentId), f.name)
        f.copyRecursively(target, overwrite = false)
        return target.absolutePath
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        val parent = File(parentDocumentId).absolutePath.trimEnd('/')
        val child = File(documentId).absolutePath
        return child == parent || child.startsWith("$parent/")
    }

    override fun getDocumentType(documentId: String): String = mimeOf(fileFor(documentId))

    override fun querySearchDocuments(rootId: String, query: String, projection: Array<out String>?): Cursor {
        val c = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        var count = 0
        Environment.getExternalStorageDirectory().walkTopDown()
            .onEnter { !it.name.startsWith(".") }
            .forEach {
                if (count < 200 && it.name.contains(query, ignoreCase = true)) {
                    addRow(c, it)
                    count++
                }
            }
        return c
    }

    /** Helper for other code: tree uri for a folder path. */
    fun treeUriFor(path: String) = DocumentsContract.buildTreeDocumentUri(authority(context!!.packageName), path)
}
