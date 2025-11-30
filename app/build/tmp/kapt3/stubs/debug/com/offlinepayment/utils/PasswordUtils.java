package com.offlinepayment.utils;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0002J&\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u00062\u0006\u0010\u0007\u001a\u00020\u00042\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u0004J\u001e\u0010\t\u001a\u00020\n2\u0006\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u0004\u00a8\u0006\f"}, d2 = {"Lcom/offlinepayment/utils/PasswordUtils;", "", "()V", "generateSalt", "", "hashPassword", "Lkotlin/Pair;", "password", "salt", "verifyPassword", "", "storedHash", "app_debug"})
public final class PasswordUtils {
    @org.jetbrains.annotations.NotNull()
    public static final com.offlinepayment.utils.PasswordUtils INSTANCE = null;
    
    private PasswordUtils() {
        super();
    }
    
    /**
     * Hash password using SHA-256 with salt for local storage
     * Note: This is for local offline verification only
     * Server uses bcrypt which is more secure
     */
    @org.jetbrains.annotations.NotNull()
    public final kotlin.Pair<java.lang.String, java.lang.String> hashPassword(@org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.Nullable()
    java.lang.String salt) {
        return null;
    }
    
    /**
     * Verify password against stored hash
     */
    public final boolean verifyPassword(@org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    java.lang.String storedHash, @org.jetbrains.annotations.NotNull()
    java.lang.String salt) {
        return false;
    }
    
    private final java.lang.String generateSalt() {
        return null;
    }
}