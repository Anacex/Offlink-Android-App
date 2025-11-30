package com.offlinepayment.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J$\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000bH\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\f\u0010\rJ\u000e\u0010\u000e\u001a\u00020\u000fH\u0086@\u00a2\u0006\u0002\u0010\u0010J$\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\u0012\u001a\u00020\u0013H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0014\u0010\u0015J\"\u0010\u0016\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u00170\bH\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0018\u0010\u0010J@\u0010\u0019\u001a\b\u0012\u0004\u0012\u0002H\u001a0\b\"\u0004\b\u0000\u0010\u001a2\u001c\u0010\u001b\u001a\u0018\b\u0001\u0012\n\u0012\b\u0012\u0004\u0012\u0002H\u001a0\u001d\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u001cH\u0082@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u001e\u0010\u001fJ$\u0010 \u001a\b\u0012\u0004\u0012\u00020!0\b2\u0006\u0010\n\u001a\u00020\"H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b#\u0010$R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006%"}, d2 = {"Lcom/offlinepayment/data/repository/WalletRepository;", "", "api", "Lcom/offlinepayment/data/network/WalletApi;", "ioDispatcher", "Lkotlinx/coroutines/CoroutineDispatcher;", "(Lcom/offlinepayment/data/network/WalletApi;Lkotlinx/coroutines/CoroutineDispatcher;)V", "createWallet", "Lkotlin/Result;", "Lcom/offlinepayment/data/WalletDto;", "request", "Lcom/offlinepayment/data/WalletCreateRequest;", "createWallet-gIAlu-s", "(Lcom/offlinepayment/data/WalletCreateRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "ensureSessionOrThrow", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getWallet", "id", "", "getWallet-gIAlu-s", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "listWallets", "", "listWallets-IoAF18A", "safeCall", "T", "block", "Lkotlin/Function1;", "Lkotlin/coroutines/Continuation;", "safeCall-gIAlu-s", "(Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "transfer", "Lcom/offlinepayment/data/WalletTransferResponse;", "Lcom/offlinepayment/data/WalletTransferRequest;", "transfer-gIAlu-s", "(Lcom/offlinepayment/data/WalletTransferRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class WalletRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.offlinepayment.data.network.WalletApi api = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineDispatcher ioDispatcher = null;
    
    public WalletRepository(@org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.network.WalletApi api, @org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineDispatcher ioDispatcher) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object ensureSessionOrThrow(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}