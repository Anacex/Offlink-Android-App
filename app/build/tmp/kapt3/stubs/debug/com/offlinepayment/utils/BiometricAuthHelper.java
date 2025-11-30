package com.offlinepayment.utils;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J*\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\nJ\u000e\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\r\u00a8\u0006\u000e"}, d2 = {"Lcom/offlinepayment/utils/BiometricAuthHelper;", "", "()V", "authenticate", "", "activity", "Landroidx/fragment/app/FragmentActivity;", "title", "", "subtitle", "(Landroidx/fragment/app/FragmentActivity;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "isBiometricAvailable", "context", "Landroid/content/Context;", "app_debug"})
public final class BiometricAuthHelper {
    @org.jetbrains.annotations.NotNull()
    public static final com.offlinepayment.utils.BiometricAuthHelper INSTANCE = null;
    
    private BiometricAuthHelper() {
        super();
    }
    
    /**
     * Check if biometric authentication is available on the device
     */
    public final boolean isBiometricAvailable(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Show biometric authentication prompt
     * Returns true if authentication successful, false otherwise
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object authenticate(@org.jetbrains.annotations.NotNull()
    androidx.fragment.app.FragmentActivity activity, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String subtitle, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
}