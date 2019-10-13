package com.busboard.busboard

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.busboard.busboard.farebot.core.nfc.CardStream
import com.busboard.busboard.farebot.core.nfc.NfcStream
import com.busboard.busboard.farebot.core.nfc.TagReaderFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

// Firebase
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val nfcStream: NfcStream
    private val cardStream: CardStream

    init {
        val tagReaderFactory = TagReaderFactory()
        this.nfcStream = NfcStream(this)
        this.cardStream = CardStream(this.nfcStream, tagReaderFactory)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.nfcStream.onCreate(this, savedInstanceState)

        // Firestore
        // Access a Cloud Firestore instance from your Activity
        val db = FirebaseFirestore.getInstance()

        // Create this user in the database
        val cardID = "123"
        val newUser = db.collection("users")
                .document(cardID)

        // Add trips to this user
        val tripID = "12345352321"
        val tripData = hashMapOf(
                "cost" to 2.75,
                "type" to "Bus",
                "transitSystem" to "King County Metro Transit",
                "transitLine" to "Coach #3699",
                "date" to "10-03-2019",
                "startTime" to "14:32"
        )

        val trip = newUser.collection("trips")
                .document(tripID)

        trip.set(tripData)
    }

    override fun onResume() {
        super.onResume()
        this.nfcStream.onResume()

        cardStream.observeCards()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { card ->
                Log.d("NFC data", card.toString())
            }

    }

    override fun onPause() {
        super.onPause()
        this.nfcStream.onPause()
    }

}
