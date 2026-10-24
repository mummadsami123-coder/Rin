package com.example.calls

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String
)

class CallManager(private val context: Context) {

    fun searchContacts(query: String): List<ContactItem> {
        val contacts = mutableListOf<ContactItem>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.let {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) else "Unknown"
                    val number = if (numberIndex != -1) it.getString(numberIndex) else ""
                    contacts.add(ContactItem(id, name, number))
                }
            }
        } catch (e: Exception) {
            Log.e("CallManager", "Error querying contacts", e)
        } finally {
            cursor?.close()
        }

        return contacts.distinctBy { it.name }
    }

    fun makeCall(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("CallManager", "Error making phone call", e)
            false
        }
    }

    companion object {
        fun resolveContactName(context: Context, phoneNumber: String): String? {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return null
            }
            if (phoneNumber.isBlank()) return null

            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            var cursor: Cursor? = null
            return try {
                cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) cursor.getString(nameIndex) else null
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("CallManager", "Error resolving contact name", e)
                null
            } finally {
                cursor?.close()
            }
        }
    }
}
