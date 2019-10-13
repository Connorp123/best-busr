package com.busboard.busboard

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.busboard.busboard.farebot.core.nfc.CardStream
import com.busboard.busboard.farebot.core.nfc.NfcStream
import com.busboard.busboard.farebot.core.nfc.TagReaderFactory
import com.codebutler.farebot.app.core.transit.TransitFactoryRegistry
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

// Firebase
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val nfcStream: NfcStream
    private val cardStream: CardStream
    private val transitFactoryRegistry: TransitFactoryRegistry

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


        // Create a new user with a first and last name
        val user = hashMapOf(
                "card" to "1234",
                "balance" to "12",
                "numTrips" to 3
//                "trips" to userTrips

        )

        // Add a new document with a generated ID
        db.collection("users")
                .add(user)
                .addOnSuccessListener { documentReference ->
                    Log.d("firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("firestore", "Error adding document", e)
                }
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
                    val trips = transitInfo?.trips

                    Log.d("NFC", transitInfo?.getSerialNumber())

                } catch (ex: Exception) {
//                    e.onError(ex)
                    Log.d("NFC", "data parse exception")
                }

            }
    }

    override fun onPause() {
        super.onPause()
        this.nfcStream.onPause()
    }

}
