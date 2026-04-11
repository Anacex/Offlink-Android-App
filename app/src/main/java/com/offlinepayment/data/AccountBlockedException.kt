package com.offlinepayment.data

/** Server returned HTTP 403 with JSON detail code ACCOUNT_BLOCKED (see [com.offlinepayment.utils.AccountBlockedParser]). */
class AccountBlockedException(message: String) : Exception(message)
