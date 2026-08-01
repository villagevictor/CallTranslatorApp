package com.example.calltranslatorapp.data

import android.content.Context
import android.provider.ContactsContract

data class ContactModel(
    val name: String,
    val phoneNumber: String
)

class ContactFetcher(private val context: Context) {

    fun getDeviceContacts(): List<ContactModel> {
        val contactList = mutableListOf<ContactModel>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = if (nameIndex != -1) it.getString(nameIndex) else "Unknown"
                val number = if (numberIndex != -1) it.getString(numberIndex) else ""
                contactList.add(ContactModel(name, number))
            }
        }
        return contactList
    }
}
