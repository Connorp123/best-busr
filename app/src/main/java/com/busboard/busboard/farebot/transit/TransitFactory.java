package com.busboard.busboard.farebot.transit;

import androidx.annotation.NonNull;

import com.busboard.busboard.farebot.card.Card;

public interface TransitFactory<C extends Card, T extends TransitInfo> {

    boolean check(@NonNull C card);

    @NonNull
    TransitIdentity parseIdentity(@NonNull C card);

    @NonNull
    T parseInfo(@NonNull C card);
}