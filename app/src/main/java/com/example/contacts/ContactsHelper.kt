package com.example.contacts

import android.Manifest
import android.content.Context
import android.provider.ContactsContract
import com.example.permissions.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val id: String,
    val name: String,
    val phone: String
)

object ContactsHelper {

    suspend fun loadPhoneContacts(context: Context): List<DeviceContact> = withContext(Dispatchers.IO) {
        if (!PermissionManager.isGranted(context, Manifest.permission.READ_CONTACTS)) {
            return@withContext emptyList()
        }

        val results = LinkedHashMap<String, DeviceContact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getString(idIdx).orEmpty() else ""
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty().trim() else ""
                    val rawPhone = if (numIdx >= 0) cursor.getString(numIdx).orEmpty().trim() else ""
                    val normalizedPhone = rawPhone.replace(Regex("[\\s\\-()]"), "")
                    if (name.isNotBlank() && normalizedPhone.isNotBlank()) {
                        val key = "$name|$normalizedPhone"
                        if (!results.containsKey(key)) {
                            results[key] = DeviceContact(id = id, name = name, phone = rawPhone)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return@withContext emptyList()
        }
        results.values.toList()
    }
}
