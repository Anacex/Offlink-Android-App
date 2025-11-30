package com.offlinepayment.utils;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\t\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\n\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\u000b"}, d2 = {"Lcom/offlinepayment/utils/NetworkUtils;", "", "()V", "isConnectedToMobileData", "", "context", "Landroid/content/Context;", "isConnectedToWifi", "isOffline", "isOnline", "isWifiConnected", "app_debug"})
public final class NetworkUtils {
    @org.jetbrains.annotations.NotNull()
    public static final com.offlinepayment.utils.NetworkUtils INSTANCE = null;
    
    private NetworkUtils() {
        super();
    }
    
    /**
     * Check if device is connected to WiFi
     */
    public final boolean isConnectedToWifi(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Check if device is connected to mobile data (cellular)
     */
    public final boolean isConnectedToMobileData(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Check if device has internet connectivity (WiFi or mobile data)
     * Returns true if connected to WiFi OR mobile data with internet access
     */
    public final boolean isOnline(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Check if device is completely offline (no WiFi and no mobile data)
     */
    public final boolean isOffline(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Check if device is connected to WiFi and has internet
     */
    public final boolean isWifiConnected(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
}