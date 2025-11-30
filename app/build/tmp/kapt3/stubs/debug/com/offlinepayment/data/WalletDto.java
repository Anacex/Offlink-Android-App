package com.offlinepayment.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0014\b\u0087\b\u0018\u00002\u00020\u0001B7\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0001\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0001\u0010\u0006\u001a\u00020\u0005\u0012\b\b\u0001\u0010\u0007\u001a\u00020\b\u0012\b\b\u0001\u0010\t\u001a\u00020\n\u00a2\u0006\u0002\u0010\u000bJ\t\u0010\u0014\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\bH\u00c6\u0003J\t\u0010\u0018\u001a\u00020\nH\u00c6\u0003J;\u0010\u0019\u001a\u00020\u00002\b\b\u0003\u0010\u0002\u001a\u00020\u00032\b\b\u0003\u0010\u0004\u001a\u00020\u00052\b\b\u0003\u0010\u0006\u001a\u00020\u00052\b\b\u0003\u0010\u0007\u001a\u00020\b2\b\b\u0003\u0010\t\u001a\u00020\nH\u00c6\u0001J\u0013\u0010\u001a\u001a\u00020\n2\b\u0010\u001b\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001c\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u001d\u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\u0012R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u000f\u00a8\u0006\u001e"}, d2 = {"Lcom/offlinepayment/data/WalletDto;", "", "id", "", "wallet_type", "", "currency", "balance", "Ljava/math/BigDecimal;", "is_active", "", "(ILjava/lang/String;Ljava/lang/String;Ljava/math/BigDecimal;Z)V", "getBalance", "()Ljava/math/BigDecimal;", "getCurrency", "()Ljava/lang/String;", "getId", "()I", "()Z", "getWallet_type", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class WalletDto {
    private final int id = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String wallet_type = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String currency = null;
    @org.jetbrains.annotations.NotNull()
    private final java.math.BigDecimal balance = null;
    private final boolean is_active = false;
    
    public WalletDto(@com.squareup.moshi.Json(name = "id")
    int id, @com.squareup.moshi.Json(name = "wallet_type")
    @org.jetbrains.annotations.NotNull()
    java.lang.String wallet_type, @com.squareup.moshi.Json(name = "currency")
    @org.jetbrains.annotations.NotNull()
    java.lang.String currency, @com.squareup.moshi.Json(name = "balance")
    @org.jetbrains.annotations.NotNull()
    java.math.BigDecimal balance, @com.squareup.moshi.Json(name = "is_active")
    boolean is_active) {
        super();
    }
    
    public final int getId() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getWallet_type() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCurrency() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.math.BigDecimal getBalance() {
        return null;
    }
    
    public final boolean is_active() {
        return false;
    }
    
    public final int component1() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.math.BigDecimal component4() {
        return null;
    }
    
    public final boolean component5() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.offlinepayment.data.WalletDto copy(@com.squareup.moshi.Json(name = "id")
    int id, @com.squareup.moshi.Json(name = "wallet_type")
    @org.jetbrains.annotations.NotNull()
    java.lang.String wallet_type, @com.squareup.moshi.Json(name = "currency")
    @org.jetbrains.annotations.NotNull()
    java.lang.String currency, @com.squareup.moshi.Json(name = "balance")
    @org.jetbrains.annotations.NotNull()
    java.math.BigDecimal balance, @com.squareup.moshi.Json(name = "is_active")
    boolean is_active) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}