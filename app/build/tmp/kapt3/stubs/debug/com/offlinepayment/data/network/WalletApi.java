package com.offlinepayment.data.network;

/**
 * Retrofit contract for wallet-related routes.
 * NOTE: Replace placeholder assumptions with generated OpenAPI models if available.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\u00020\u00032\b\b\u0001\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00030\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u0018\u0010\u000e\u001a\u00020\u000f2\b\b\u0001\u0010\u0004\u001a\u00020\u0010H\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u000f0\fH\u00a7@\u00a2\u0006\u0002\u0010\r\u00a8\u0006\u0013"}, d2 = {"Lcom/offlinepayment/data/network/WalletApi;", "", "createWallet", "Lcom/offlinepayment/data/WalletDto;", "request", "Lcom/offlinepayment/data/WalletCreateRequest;", "(Lcom/offlinepayment/data/WalletCreateRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getWallet", "walletId", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "listWallets", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "transfer", "Lcom/offlinepayment/data/WalletTransferResponse;", "Lcom/offlinepayment/data/WalletTransferRequest;", "(Lcom/offlinepayment/data/WalletTransferRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "transferHistory", "app_debug"})
public abstract interface WalletApi {
    
    @retrofit2.http.POST(value = "api/v1/wallets/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object createWallet(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.WalletCreateRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.offlinepayment.data.WalletDto> $completion);
    
    @retrofit2.http.GET(value = "api/v1/wallets/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object listWallets(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.offlinepayment.data.WalletDto>> $completion);
    
    @retrofit2.http.GET(value = "api/v1/wallets/{id}")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getWallet(@retrofit2.http.Path(value = "id")
    int walletId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.offlinepayment.data.WalletDto> $completion);
    
    @retrofit2.http.POST(value = "api/v1/wallets/transfer")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object transfer(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.WalletTransferRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.offlinepayment.data.WalletTransferResponse> $completion);
    
    @retrofit2.http.GET(value = "api/v1/wallets/transfers/history")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object transferHistory(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.offlinepayment.data.WalletTransferResponse>> $completion);
}