package com.offlinepayment.security

import android.content.Context

/**
 * Stores the server-provided "next link" for this device's offline ledger chain.
 *
 * On a fresh install (empty local DB), using the server head prevents the first sync after reinstall
 * from looking like a chain break when the device fingerprint is stable.
 *
 * If the device fingerprint changes (new install generates a new one), the server will return genesis.
 */
object DeviceLedgerSeedStore {
    private const val PREFS = "device_ledger_seed_store"
    private const val KEY_PREV = "prev_hash"
    private const val KEY_NEXT_SEQ = "next_seq"

    data class Seed(val prevHash: String, val nextSequence: Long)

    fun save(context: Context, prevHash: String?, nextSequence: Long?) {
        if (prevHash.isNullOrBlank() || nextSequence == null || nextSequence < 1L) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREV, prevHash.trim())
            .putLong(KEY_NEXT_SEQ, nextSequence)
            .apply()
    }

    fun load(context: Context): Seed? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prev = prefs.getString(KEY_PREV, null)?.trim().orEmpty()
        val seq = prefs.getLong(KEY_NEXT_SEQ, -1L)
        if (prev.isBlank() || seq < 1L) return null
        return Seed(prevHash = prev, nextSequence = seq)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PREV)
            .remove(KEY_NEXT_SEQ)
            .apply()
    }
}

