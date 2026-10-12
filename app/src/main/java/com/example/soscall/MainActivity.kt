package com.example.soscall

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity


import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location



class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_CONTACT = 1
    private lateinit var sharedPreferences: SharedPreferences
    private var currentLevel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Kontrola
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.SEND_SMS
                ),
                2
            )
        }

        // sharedPreferences
        sharedPreferences = getSharedPreferences("SOSApp", MODE_PRIVATE)

        val contactsWarning = findViewById<TextView>(R.id.contacts_warning)
        val contactsAlarm = findViewById<TextView>(R.id.contacts_alarm)
        val contactsUrgent = findViewById<TextView>(R.id.contacts_urgent)

        val addContactWarning = findViewById<Button>(R.id.add_contact_warning)
        val addContactAlarm = findViewById<Button>(R.id.add_contact_alarm)
        val addContactUrgent = findViewById<Button>(R.id.add_contact_urgent)

        val sendWarning = findViewById<Button>(R.id.btn_warning)
        val sendAlarm = findViewById<Button>(R.id.btn_alarm)
        val sendUrgent = findViewById<Button>(R.id.btn_urgent)

        // pridanie kontaktu
        addContactWarning.setOnClickListener { openContactPicker("Warning") }
        addContactAlarm.setOnClickListener { openContactPicker("Alarm") }
        addContactUrgent.setOnClickListener { openContactPicker("Urgent") }

        // odoslanie SMS
        sendWarning.setOnClickListener { sendSMS("Warning") }
        sendAlarm.setOnClickListener { sendSMS("Alarm") }
        sendUrgent.setOnClickListener { sendSMS("Urgent") }

        // Načítanie a zobrazenie kontaktov zo SharedPreferences
        updateContactsDisplay("Warning", contactsWarning)
        updateContactsDisplay("Alarm", contactsAlarm)
        updateContactsDisplay("Urgent", contactsUrgent)

        // na odstránenie kontaktov
        contactsWarning.setOnLongClickListener {
            showRemoveContactDialog("Warning", contactsWarning)
            true
        }
        contactsAlarm.setOnLongClickListener {
            showRemoveContactDialog("Alarm", contactsAlarm)
            true
        }
        contactsUrgent.setOnLongClickListener {
            showRemoveContactDialog("Urgent", contactsUrgent)
            true
        }
    }

    private fun updateContactsDisplay(level: String, textView: TextView) {
        val numbers = sharedPreferences.getStringSet(level, mutableSetOf())
        println("Načítané kontakty pre $level: $numbers")
        if (numbers.isNullOrEmpty()) {
            textView.text = "Vybrané kontakty: (žiadne)"
        } else {
            textView.text = "Vybrané kontakty:\n${numbers.joinToString("\n")}"
        }
    }

    private fun openContactPicker(level: String) {
        currentLevel = level
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(intent, REQUEST_CONTACT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONTACT && resultCode == Activity.RESULT_OK) {
            data?.data?.let { contactUri ->
                val cursor = contentResolver.query(contactUri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val name = it.getString(nameIndex) ?: "Neznáme meno"
                        val number = it.getString(numberIndex)

                        // Uloženie formátu "Meno - Číslo"
                        saveNumberToLevel(currentLevel!!, "$name - $number")
                        Toast.makeText(this, "Pridané: $name - $number", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveNumberToLevel(level: String, number: String) {
        val numbers = sharedPreferences.getStringSet(level, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (numbers.size < 3) {
            numbers.add(number)
            sharedPreferences.edit().putStringSet(level, numbers).apply()

            // Aktualizácia zobrazenia
            val textView = when (level) {
                "Warning" -> findViewById<TextView>(R.id.contacts_warning)
                "Alarm" -> findViewById<TextView>(R.id.contacts_alarm)
                "Urgent" -> findViewById<TextView>(R.id.contacts_urgent)
                else -> null
            }
            textView?.let { updateContactsDisplay(level, it) }

            Toast.makeText(this, "Kontakt $number bol pridaný.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Môžeš vybrať maximálne 3 kontakty!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRemoveContactDialog(level: String, textView: TextView) {
        val numbers = sharedPreferences.getStringSet(level, mutableSetOf())?.toMutableSet()

        if (numbers.isNullOrEmpty()) {
            Toast.makeText(this, "Žiadne kontakty na odstránenie!", Toast.LENGTH_SHORT).show()
            return
        }

        val numbersArray = numbers.toTypedArray()

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Vyberte kontakt na odstránenie")
        builder.setItems(numbersArray) { _, which ->
            val selectedContact = numbersArray[which]
            if (numbers.remove(selectedContact)) {
                sharedPreferences.edit().putStringSet(level, numbers).apply()
                updateContactsDisplay(level, textView)
                Toast.makeText(this, "Kontakt $selectedContact bol odstránený.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Nepodarilo sa odstrániť kontakt.", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Zrušiť", null)
        builder.create().show()
    }





    private fun getCurrentLocation(callback: (latitude: Double, longitude: Double) -> Unit) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Aplikácia nemá povolenie na prístup k polohe.", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                callback(location.latitude, location.longitude)
            } else {
                Toast.makeText(this, "Nepodarilo sa získať GPS polohu.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Chyba pri získavaní GPS polohy: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun sendSMS(level: String) {
        val numbers = sharedPreferences.getStringSet(level, mutableSetOf())

        if (numbers.isNullOrEmpty()) {
            Toast.makeText(this, "Žiadne kontakty na poslanie správy.", Toast.LENGTH_SHORT).show()
            return
        }

        getCurrentLocation { latitude, longitude ->
            val message = "Emergency Level: $level\nGPS Location: Latitude=$latitude, Longitude=$longitude"
            val smsManager = SmsManager.getDefault()
            for (number in numbers) {
                val phoneNumber = number.split(" - ").last()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(this, "Správa odoslaná na $number.", Toast.LENGTH_SHORT).show()
            }
        }
    }



}
