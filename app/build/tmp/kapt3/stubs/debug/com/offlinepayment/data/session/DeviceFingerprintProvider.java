package com.offlinepayment.data.session;

/**
 * Generates and persists a pseudo device fingerprint for API calls.
 * NOTE: Replace with hardware-backed fingerprinting before production rollout.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\u0007\u001a\u00020\u0004J\u000e\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/offlinepayment/data/session/DeviceFingerprintProvider;", "", "()V", "KEY_FINGERPRINT", "", "PREFS_NAME", "cachedFingerprint", "getFingerprint", "initialize", "", "context", "Landroid/content/Context;", "app_debug"})
public final class DeviceFingerprintProvider {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS_NAME = "device_fingerprint_store";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_FINGERPRINT = "fingerprint";
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile java.lang.String cachedFingerprint;
    @org.jetbrains.annotations.NotNull()
    public static final com.offlinepayment.data.session.DeviceFingerprintProvider INSTANCE = null;
    
    private DeviceFingerprintProvider() {
        super();
    }
    
    public final void initialize(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFingerprint() {
        return null;
    }
}