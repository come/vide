package com.vide.data

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import com.vide.model.ContactAction
import com.vide.model.ContactActionType

class ContactSearcher(private val context: Context) {

    /** Fast local search — phones + local emails */
    fun search(query: String): List<ContactAction> {
        if (query.length < 2) return emptyList()

        val actions = mutableListOf<ContactAction>()
        val whatsappInstalled = isAppInstalled("com.whatsapp")

        val phones = queryPhones(query)
        phones.forEach { (name, number) ->
            actions.add(ContactAction(name, "appeler", ContactActionType.CALL, number, "call_$number"))
            actions.add(ContactAction(name, "sms", ContactActionType.SMS, number, "sms_$number"))
            if (whatsappInstalled) {
                actions.add(ContactAction(name, "whatsapp", ContactActionType.WHATSAPP, number, "wa_$number"))
            }
        }

        val emails = queryEmails(query)
        emails.forEach { (name, email) ->
            actions.add(ContactAction(name, "email", ContactActionType.EMAIL, email, "email_$email"))
        }

        return actions
    }

    private fun queryPhones(query: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            Uri.encode(query)
        )
        val cursor = try {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )
        } catch (_: Exception) { null }

        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val seen = mutableSetOf<String>()

            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: continue
                val number = it.getString(numberIdx) ?: continue
                if (seen.add(name.lowercase())) {
                    results.add(name to number)
                }
            }
        }

        return results
    }

    private fun queryEmails(query: String): List<Pair<String, String>> {
        val seen = mutableSetOf<String>()
        val results = mutableListOf<Pair<String, String>>()

        // 1. CONTENT_FILTER_URI — prefix match on name and email (fast, standard)
        val filterUri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI,
            Uri.encode(query)
        )
        try {
            context.contentResolver.query(
                filterUri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                ),
                null, null, null
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
                val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (cursor.moveToNext()) {
                    val rawName = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val email = (if (emailIdx >= 0) cursor.getString(emailIdx) else null) ?: continue
                    val name = rawName ?: email
                    if (seen.add(email.lowercase())) {
                        results.add(name to email)
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. LIKE query — substring match on name OR email address
        try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data.DISPLAY_NAME,
                    ContactsContract.Data.DATA1
                ),
                "${ContactsContract.Data.MIMETYPE} = ? AND " +
                    "(${ContactsContract.Data.DISPLAY_NAME} LIKE ? OR ${ContactsContract.Data.DATA1} LIKE ?)",
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    "%$query%",
                    "%$query%"
                ),
                null
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
                val emailIdx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
                while (cursor.moveToNext()) {
                    val rawName = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val email = (if (emailIdx >= 0) cursor.getString(emailIdx) else null) ?: continue
                    val name = rawName ?: email
                    if (seen.add(email.lowercase())) {
                        results.add(name to email)
                    }
                }
            }
        } catch (_: Exception) {}

        return results
    }

private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

}
