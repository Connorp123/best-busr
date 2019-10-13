package com.busboard.busboard

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.busboard.busboard.core.nfc.CardStream
import com.busboard.busboard.core.nfc.NfcStream
import com.busboard.busboard.core.nfc.TagReaderFactory
import io.reactivex.schedulers.Schedulers
import io.reactivex.android.schedulers.AndroidSchedulers

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
            .subscribe { card ->
                Log.d("NFC data", card.toString())
            }

    }

    override fun onPause() {
        super.onPause()
        this.nfcStream.onPause()
    }

}
