package com.ustad.personalassistant.files

/** UI-independent contracts for modern Android Photo Picker / Storage Access Framework. */
interface FilePickerCapabilities {
    fun selectPhoto()
    fun selectFile()
    fun selectMultipleFiles()
}

data class SelectedFile(val uri: String, val mimeType: String? = null)

object FilePickerMimeTypes {
    const val ANY = "*/*"
    const val IMAGE = "image/*"
}
