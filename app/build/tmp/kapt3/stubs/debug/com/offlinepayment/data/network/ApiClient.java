package com.offlinepayment.data.network;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u00c7\u0002\u0018\u00002\u00020\u0001:\u0001\u0019B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u001b\u0010\u0003\u001a\u00020\u00048FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0007\u0010\b\u001a\u0004\b\u0005\u0010\u0006R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\r\u001a\n \u000f*\u0004\u0018\u00010\u000e0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0012\u001a\n \u000f*\u0004\u0018\u00010\u00130\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0014\u001a\u00020\u00158FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0018\u0010\b\u001a\u0004\b\u0016\u0010\u0017\u00a8\u0006\u001a"}, d2 = {"Lcom/offlinepayment/data/network/ApiClient;", "", "()V", "authApi", "Lcom/offlinepayment/data/network/AuthApi;", "getAuthApi", "()Lcom/offlinepayment/data/network/AuthApi;", "authApi$delegate", "Lkotlin/Lazy;", "authHeaderInterceptor", "Lokhttp3/Interceptor;", "loggingInterceptor", "Lokhttp3/logging/HttpLoggingInterceptor;", "moshi", "Lcom/squareup/moshi/Moshi;", "kotlin.jvm.PlatformType", "okHttp", "Lokhttp3/OkHttpClient;", "retrofit", "Lretrofit2/Retrofit;", "walletApi", "Lcom/offlinepayment/data/network/WalletApi;", "getWalletApi", "()Lcom/offlinepayment/data/network/WalletApi;", "walletApi$delegate", "BigDecimalAdapter", "app_debug"})
public final class ApiClient {
    @org.jetbrains.annotations.NotNull()
    private static final okhttp3.Interceptor authHeaderInterceptor = null;
    @org.jetbrains.annotations.NotNull()
    private static final okhttp3.logging.HttpLoggingInterceptor loggingInterceptor = null;
    @org.jetbrains.annotations.NotNull()
    private static final okhttp3.OkHttpClient okHttp = null;
    private static final com.squareup.moshi.Moshi moshi = null;
    private static final retrofit2.Retrofit retrofit = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.Lazy authApi$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.Lazy walletApi$delegate = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.offlinepayment.data.network.ApiClient INSTANCE = null;
    
    private ApiClient() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.offlinepayment.data.network.AuthApi getAuthApi() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.offlinepayment.data.network.WalletApi getWalletApi() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0012\u0010\u0004\u001a\u0004\u0018\u00010\u00022\u0006\u0010\u0005\u001a\u00020\u0006H\u0017J\u001a\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\u0002H\u0017\u00a8\u0006\f"}, d2 = {"Lcom/offlinepayment/data/network/ApiClient$BigDecimalAdapter;", "Lcom/squareup/moshi/JsonAdapter;", "Ljava/math/BigDecimal;", "()V", "fromJson", "reader", "Lcom/squareup/moshi/JsonReader;", "toJson", "", "writer", "Lcom/squareup/moshi/JsonWriter;", "value", "app_debug"})
    static final class BigDecimalAdapter extends com.squareup.moshi.JsonAdapter<java.math.BigDecimal> {
        
        public BigDecimalAdapter() {
            super();
        }
        
        @com.squareup.moshi.FromJson()
        @java.lang.Override()
        @org.jetbrains.annotations.Nullable()
        public java.math.BigDecimal fromJson(@org.jetbrains.annotations.NotNull()
        com.squareup.moshi.JsonReader reader) {
            return null;
        }
        
        @com.squareup.moshi.ToJson()
        @java.lang.Override()
        public void toJson(@org.jetbrains.annotations.NotNull()
        com.squareup.moshi.JsonWriter writer, @org.jetbrains.annotations.Nullable()
        java.math.BigDecimal value) {
        }
    }
}