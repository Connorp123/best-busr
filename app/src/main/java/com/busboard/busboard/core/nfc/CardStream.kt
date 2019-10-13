/*
 * CardStream.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.busboard.busboard.core.nfc

import com.busboard.busboard.core.kotlin.Optional

import com.busboard.busboard.core.kotlin.filterAndGetOptional
import com.busboard.busboard.core.sample.RawSampleCard
import com.busboard.busboard.card.RawCard
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class CardStream(
    private val nfcStream: NfcStream,
    private val tagReaderFactory: TagReaderFactory
) {

    private val loadingRelay: BehaviorRelay<Boolean> = BehaviorRelay.createDefault(false)
    private val errorRelay: PublishRelay<Throwable> = PublishRelay.create()
    private val sampleRelay: PublishRelay<RawCard<*>> = PublishRelay.create()

    fun observeCards(): Observable<RawCard<*>> {
        val realCards = nfcStream.observe()
            .observeOn(Schedulers.io())
            .doOnNext { loadingRelay.accept(true) }
            .map { tag ->
                Optional(
                    try {
                        val rawCard = tagReaderFactory.getTagReader(tag.id, tag).readTag()
                        if (rawCard.isUnauthorized) {
                            throw CardUnauthorizedException()
                        }
                        rawCard
                    } catch (error: Throwable) {
                        errorRelay.accept(error)
                        loadingRelay.accept(false)
                        null
                    }
                )
            }
            .filterAndGetOptional()

        val sampleCards = sampleRelay
            .observeOn(Schedulers.io())
            .doOnNext { loadingRelay.accept(true) }
            .delay(3, TimeUnit.SECONDS)

        return Observable.merge(realCards, sampleCards)
            .doOnNext { loadingRelay.accept(false) }
    }

    fun observeLoading(): Observable<Boolean> = loadingRelay.hide()

    fun observeErrors(): Observable<Throwable> = errorRelay.hide()

    fun emitSample() {
        sampleRelay.accept(RawSampleCard())
    }

    class CardUnauthorizedException : Throwable() {
        override val message: String?
            get() = "Unauthorized"
    }
}
