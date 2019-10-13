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

class MainActivity : AppCompatActivity() {

    private val nfcStream: NfcStream
    private var cardStream: CardStream

    init {
        val tagReaderFactory = TagReaderFactory()
        this.nfcStream = NfcStream(this)
        this.cardStream = CardStream(this.nfcStream, tagReaderFactory)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Startup NFC reader
        this.nfcStream.onCreate(this, savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("NFC", "WE RECEIVED A MESSAGE: " + data.toString())
        // TODO: handle it!
//        handler.post {
//            activityResultRelay.accept(ActivityResult(requestCode, resultCode, data))
//        }
    }

    override fun onResume() {
        super.onResume()
        this.nfcStream.onResume()
        cardStream.observeCards()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { card ->
                Log.d("NFC", card.toString())
            }
    }

    override fun onPause() {
        super.onPause()
        this.nfcStream.onPause()
    }

}
