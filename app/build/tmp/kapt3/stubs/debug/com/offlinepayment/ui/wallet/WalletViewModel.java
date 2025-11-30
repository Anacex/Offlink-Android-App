package com.offlinepayment.ui.wallet;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \u00162\u00020\u0001:\u0001\u0016B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u000e\u001a\u00020\u000fJ\u001e\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u0014\u001a\u00020\u0015R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\b\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00070\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0017"}, d2 = {"Lcom/offlinepayment/ui/wallet/WalletViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/offlinepayment/data/repository/WalletRepository;", "(Lcom/offlinepayment/data/repository/WalletRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/offlinepayment/ui/wallet/WalletUiState;", "sessionJob", "Lkotlinx/coroutines/Job;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "refreshWallets", "", "transfer", "fromWalletId", "", "toWalletId", "amount", "Ljava/math/BigDecimal;", "Companion", "app_debug"})
public final class WalletViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.offlinepayment.data.repository.WalletRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.offlinepayment.ui.wallet.WalletUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.offlinepayment.ui.wallet.WalletUiState> uiState = null;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job sessionJob;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.lifecycle.ViewModelProvider.Factory Factory = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.offlinepayment.ui.wallet.WalletViewModel.Companion Companion = null;
    
    public WalletViewModel(@org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.repository.WalletRepository repository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.offlinepayment.ui.wallet.WalletUiState> getUiState() {
        return null;
    }
    
    public final void refreshWallets() {
    }
    
    public final void transfer(int fromWalletId, int toWalletId, @org.jetbrains.annotations.NotNull()
    java.math.BigDecimal amount) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/offlinepayment/ui/wallet/WalletViewModel$Companion;", "", "()V", "Factory", "Landroidx/lifecycle/ViewModelProvider$Factory;", "getFactory", "()Landroidx/lifecycle/ViewModelProvider$Factory;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.lifecycle.ViewModelProvider.Factory getFactory() {
            return null;
        }
    }
}