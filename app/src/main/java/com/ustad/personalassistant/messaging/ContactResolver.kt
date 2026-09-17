package com.ustad.personalassistant.messaging

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class AndroidContactResolver(private val context: Context) : ContactResolver {
    override fun resolve(nameOrNumber: String): List<ContactMatch> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return emptyList()
        val query = nameOrNumber.trim()
        if (query.isBlank()) return emptyList()
        val results = linkedMapOf<String, ContactMatch>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(uri, projection, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex).orEmpty()
                val number = cursor.getString(numberIndex).orEmpty()
                if (name.isBlank() || number.isBlank()) continue
                val normalizedQuery = query.lowercase()
                val matches = name.lowercase() == normalizedQuery || name.lowercase().contains(normalizedQuery) || number.filter(Char::isDigit).endsWith(query.filter(Char::isDigit))
                if (matches) {
                    val key = "$name|${number.filter(Char::isDigit)}"
                    results.putIfAbsent(key, ContactMatch(name, number, if (idIndex >= 0) cursor.getLong(idIndex) else null))
                }
            }
        }
        return results.values.toList().take(10)
    }
}
