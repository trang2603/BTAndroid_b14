package com.demo

import android.app.AlertDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.demo.databinding.ActivityMainBinding
import com.demo.databinding.DialogAddContactBinding
import com.demo.databinding.DialogUpdateContactBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactsAdapter
    private lateinit var contactList: MutableList<Contact>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactList = getContacts(this).toMutableList()

        // Khởi tạo danh bạ và adapter
        contactAdapter =
            ContactsAdapter(
                context = this,
                contactList,
                onItemClick = { contact ->
                    showUpdateDialog(contact)
                },
                onConvertClick = {
                    binding.btnConvert.isEnabled = contactAdapter.isAnyChecked()
                },
            )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = contactAdapter

        binding.fab.setOnClickListener {
            showAddContactDialog()
        }

        binding.btnConvert.setOnClickListener {
            convertSelectedContacts()
        }
    }

    // Hiển thị dialog để thêm liên hệ mới
    private fun showAddContactDialog() {
        val dialogBinding = DialogAddContactBinding.inflate(LayoutInflater.from(this))
        val builder =
            AlertDialog
                .Builder(this)
                .setView(dialogBinding.root)
                .setTitle("Add Contact")

        builder.setPositiveButton("Add") { _, _ ->
            val name = dialogBinding.editName.text.toString()
            val phoneNumber = dialogBinding.editPhone.text.toString()
            if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                addContact(this, name, phoneNumber)
                refreshContacts()
            }
        }

        builder.setNegativeButton("Cancel", null)

        builder.create().show()
    }

    private fun addContact(
        context: Context,
        name: String,
        phoneNumber: String,
    ) {
        val contentResolver = context.contentResolver

        val values =
            ContentValues().apply {
                put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
                put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
            }
        val uri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values)
        val rawContactId = ContentUris.parseId(uri!!)

        // Tên
        val nameValues =
            ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            }
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)

        // Số điện thoại
        val phoneValues =
            ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            }
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)
        /*// Thêm liên hệ vào danh sách hiển thị và cập nhật RecyclerView
        val newContact = Contact(id = rawContactId, name = name, phoneNumber = phoneNumber)
        contactList.add(newContact)
        contactAdapter.notifyItemInserted(contactList.size - 1)*/
    }

    // Hiển thị dialog để cập nhật liên hệ
    private fun showUpdateDialog(contact: Contact) {
        val dialogBinding = DialogUpdateContactBinding.inflate(LayoutInflater.from(this))
        val builder =
            AlertDialog
                .Builder(this)
                .setView(dialogBinding.root)
                .setTitle("Update Contact")

        dialogBinding.editName.setText(contact.name)
        dialogBinding.editPhone.setText(contact.phoneNumber)

        builder.setPositiveButton("Update") { _, _ ->
            val newName = dialogBinding.editName.text.toString()
            val newPhoneNumber = dialogBinding.editPhone.text.toString()
            updateContact(this, contact.id, newName, newPhoneNumber)
            refreshContacts()
        }

        builder.setNegativeButton("Cancel", null)

        builder.create().show()
    }

    private fun updateContact(
        context: Context,
        contactId: Long,
        newName: String,
        newPhoneNumber: String,
    ) {
        val contentResolver = context.contentResolver

        // Cập nhật tên
        val nameValues =
            ContentValues().apply {
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
            }
        contentResolver.update(
            ContactsContract.Data.CONTENT_URI,
            nameValues,
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
        )

        // Cập nhật số điện thoại
        val phoneValues =
            ContentValues().apply {
                put(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhoneNumber)
            }
        contentResolver.update(
            ContactsContract.Data.CONTENT_URI,
            phoneValues,
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
        )
    }

    fun showDeleteDialog(contact: Contact) {
        val builder =
            AlertDialog
                .Builder(this)
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete ${contact.name}?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteContact(this, contact.id)
                    refreshContacts()
                }.setNegativeButton("Cancel", null)
        builder.create().show()
    }

    private fun deleteContact(
        context: Context,
        contactId: Long,
    ) {
        val contentResolver = context.contentResolver
        contentResolver.delete(
            ContactsContract.RawContacts.CONTENT_URI,
            "${ContactsContract.RawContacts.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
        )
    }

    private fun refreshContacts() {
        contactList.clear()
        contactList.addAll(getContacts(this))
        contactAdapter.notifyDataSetChanged()
    }

    private fun convertSelectedContacts() {
        /*for ((index, contact) in contactList.withIndex()) {
            if (contactAdapter.isAnyChecked()) {
                contactList[index].phoneNumber = convertPhoneNumber(contact.phoneNumber)
                updateContact(this, contact.id, contact.name, convertPhoneNumber((contact.phoneNumber)))
            }
        }
        contactAdapter.notifyDataSetChanged()*/

        val selectedContacts = contactAdapter.getSelectedContacts()
        for (contact in selectedContacts) {
            val newPhoneNumber = convertPhoneNumber(contact.phoneNumber)
            contact.phoneNumber = newPhoneNumber
            // Cập nhật lại contact với số điện thoại mới
            updateContact(this, contact.id, contact.name, newPhoneNumber)
        }
        contactAdapter.notifyDataSetChanged()
    }

    private fun convertPhoneNumber(phoneNumber: String): String =
        when {
            phoneNumber.startsWith("016") -> phoneNumber.replaceFirst("016", "03")
            phoneNumber.startsWith("8416") -> phoneNumber.replaceFirst("8416", "03")
            else -> phoneNumber
        }

    private fun getContacts(context: Context): List<Contact> {
        val contactList = mutableListOf<Contact>()
        val contentResolver = context.contentResolver
        val cursor: Cursor? =
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                null,
            )
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: "No name"
                val phoneNumber = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: "No phone number"
                contactList.add(Contact(id, name, phoneNumber))
            }
        } ?: run {
            // Xử lý khi cursor là null
            Log.e("getContacts", "Cursor is null, unable to query contacts")
        }
        return contactList
    }
}
