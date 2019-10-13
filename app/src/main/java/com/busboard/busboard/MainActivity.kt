package com.busboard.busboard

import android.os.Bundle
import android.util.Log
import android.widget.Adapter
import androidx.appcompat.app.AppCompatActivity
import com.busboard.busboard.farebot.core.nfc.CardStream
import com.busboard.busboard.farebot.core.nfc.NfcStream
import com.busboard.busboard.farebot.core.nfc.TagReaderFactory
import com.codebutler.farebot.app.core.transit.TransitFactoryRegistry
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import android.widget.ArrayAdapter
import android.widget.ListView

// Firebase
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val nfcStream: NfcStream
    private val cardStream: CardStream
    private val transitFactoryRegistry: TransitFactoryRegistry

    var array = arrayOf(" ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ")

    init {
        val tagReaderFactory = TagReaderFactory()
        this.nfcStream = NfcStream(this)
        this.cardStream = CardStream(this.nfcStream, tagReaderFactory)
        this.transitFactoryRegistry = TransitFactoryRegistry()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.nfcStream.onCreate(this, savedInstanceState)

        // Firestore
        // Access a Cloud Firestore instance from your Activity
        val db = FirebaseFirestore.getInstance()

        // NEW

        // Create a map with userID, trips
        val userTripsMap = hashMapOf<String, Int>()

        // Get list of all user references
        db.collection("users")
                .get()
                .addOnSuccessListener { result ->
                    Log.d("DB", "Got all users")
                    // For each user
                    for (user in result) {

                        // Count the number of trips
                        user.reference.collection("trips").get()
                                .addOnSuccessListener { res ->
                                    Log.d("DB", "Got user"+res.toString())
                                    // Add a map entry with this user id and trips
                                    userTripsMap[user.id] = res.documents.size
                                }
                                .addOnFailureListener { exception ->
                                    Log.d("firestore", "Error getting documents: ", exception)
                                }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("firestore", "Error getting documents: ", exception)
                }

        // Sort the map in decr order by num trips
        val sortedUsers = userTripsMap.toList().sortedBy { (_, value) -> value}.toMap()

        // Display the list
        val adapter = ArrayAdapter(this,
            R.layout.listview_item, array)

        val listView:ListView = findViewById(R.id.listview_1)
        listView.setAdapter(adapter)

        Log.d("DB", sortedUsers.toString())
        var i = 0
        for (entry in sortedUsers) {
            Log.d("DB","Key: " + entry.key)
            Log.d("DB"," Value: " + entry.value)
            array.set(i, entry.key)
            i = i + 1
        }
        adapter.notifyDataSetChanged()



    }

    override fun onResume() {
        super.onResume()
        this.nfcStream.onResume()
        cardStream.observeCards()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { rawCard ->
                Log.d("NFC raw card", rawCard.toString())
                try {
                    val card = rawCard.parse()
                    val transitInfo = transitFactoryRegistry.parseTransitInfo(card)
                    val db = FirebaseFirestore.getInstance()

                    val userID = transitInfo?.getSerialNumber()
                    if (userID != null) {
//                        array.clear()
//                        array.set(0,"New City")
                        val user = db.collection("users").document(userID)

                        val trips = transitInfo?.getTrips()

                        if (trips != null) {
                            for (trip in trips) {
                                val tripTimestamp = trip.getTimestamp().toString()
                                val tripData = hashMapOf(
                                    "time" to tripTimestamp
//                                    "balance" to trip.getBalanceString()
                                )

                                user.collection("trips")
                                    .document(tripTimestamp).set(tripData)

                            }

                        }
                    }




                    Log.d("NFC", transitInfo?.getSerialNumber())
                } catch (ex: Exception) {
                    Log.d("NFC", "data parse exception")
                }

            }
    }

    override fun onPause() {
        super.onPause()
        this.nfcStream.onPause()
    }

}
