package com.demo

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.content.ContentProviderOperation
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.convertButton.setOnClickListener {
            checkPermission()
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this,READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this, WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(READ_CONTACTS, WRITE_CONTACTS), 1)
        } else {
            loadContacts()
        }
    }

    private fun loadContacts() {
        val contactsList  = mutableListOf<Pair<String, String>>()
        val contentResolver = contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null,
        )
        cursor?.let {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val contactId = it.getString(idIndex)
                val number = it.getString(numberIndex)
                contactsList.add(Pair(contactId, number))
            }
            it.close()
        }
        convertPhoneNumbers(contactsList)
    }

    private fun convertPhoneNumbers(contactsList: List<Pair<String, String>>) {
        val operations = ArrayList<ContentProviderOperation>()

        contactsList.forEach { (contactId, phoneNumber) ->
            // Loại bỏ tất cả khoảng trắng và ký tự không phải là số
            val cleanedNumber = phoneNumber.replace(Regex("[^0-9]"), "")

            val convertedNumber = when {
                cleanedNumber.startsWith("016") -> "03${cleanedNumber.substring(3)}"
                cleanedNumber.startsWith("8416") -> "03${cleanedNumber.substring(4)}"
                else -> null
            }

            if (convertedNumber != null) {
                operations.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ? AND ${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                            arrayOf(contactId, phoneNumber)
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, convertedNumber)
                        .build()
                )
            }
        }

        try {
            // Áp dụng các thay đổi vào danh bạ
            contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            binding.contactsTextView.text = "Phone numbers converted successfully."
        } catch (e: Exception) {
            e.printStackTrace()
            binding.contactsTextView.text = "Failed to convert phone numbers."
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            loadContacts()
        }
    }
}