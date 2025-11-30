package com.offlinepayment.ui.auth;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0010\b\u0007\u0018\u0000 #2\u00020\u0001:\u0001#B\u0019\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u000b2\u0006\u0010\u0016\u001a\u00020\u000bH\u0002J\u000e\u0010\u0017\u001a\u00020\u00142\u0006\u0010\u0018\u001a\u00020\u000bJ\u0006\u0010\u0019\u001a\u00020\u0014J\u0006\u0010\u001a\u001a\u00020\u0014J\b\u0010\u001b\u001a\u00020\u0014H\u0002J\u000e\u0010\u001c\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u000bJ\u0006\u0010\u001d\u001a\u00020\u0014J&\u0010\u001e\u001a\u00020\u00142\u0006\u0010\u001f\u001a\u00020\u000b2\u0006\u0010\u0015\u001a\u00020\u000b2\u0006\u0010\u0016\u001a\u00020\u000b2\u0006\u0010 \u001a\u00020\u000bJ\u0016\u0010!\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u000b2\u0006\u0010\u0016\u001a\u00020\u000bJ\u0016\u0010\"\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u000b2\u0006\u0010\u0018\u001a\u00020\u000bR\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0004\u001a\u0004\u0018\u00010\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\t0\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006$"}, d2 = {"Lcom/offlinepayment/ui/auth/AuthViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/offlinepayment/data/repository/AuthRepository;", "context", "Landroid/content/Context;", "(Lcom/offlinepayment/data/repository/AuthRepository;Landroid/content/Context;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/offlinepayment/ui/auth/AuthUiState;", "pendingLoginEmail", "", "pendingNonce", "pendingSignupEmail", "pendingSignupPassword", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "autoLoginAfterSignup", "", "email", "password", "confirmOtp", "otp", "hideEmailVerificationDialog", "logout", "refreshUserInfo", "requestEmailVerificationOtp", "showEmailVerificationDialog", "signup", "name", "phone", "startLogin", "verifyEmail", "Companion", "app_debug"})
public final class AuthViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.offlinepayment.data.repository.AuthRepository repository = null;
    @org.jetbrains.annotations.Nullable()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.offlinepayment.ui.auth.AuthUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.offlinepayment.ui.auth.AuthUiState> uiState = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String pendingLoginEmail;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String pendingNonce;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String pendingSignupEmail;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String pendingSignupPassword;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.lifecycle.ViewModelProvider.Factory Factory = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.offlinepayment.ui.auth.AuthViewModel.Companion Companion = null;
    
    public AuthViewModel(@org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.repository.AuthRepository repository, @org.jetbrains.annotations.Nullable()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.offlinepayment.ui.auth.AuthUiState> getUiState() {
        return null;
    }
    
    private final void refreshUserInfo() {
    }
    
    public final void signup(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    java.lang.String phone) {
    }
    
    public final void verifyEmail(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String otp) {
    }
    
    private final void autoLoginAfterSignup(java.lang.String email, java.lang.String password) {
    }
    
    public final void showEmailVerificationDialog() {
    }
    
    public final void hideEmailVerificationDialog() {
    }
    
    public final void requestEmailVerificationOtp(@org.jetbrains.annotations.NotNull()
    java.lang.String email) {
    }
    
    public final void startLogin(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
    }
    
    public final void confirmOtp(@org.jetbrains.annotations.NotNull()
    java.lang.String otp) {
    }
    
    public final void logout() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\tR\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\n"}, d2 = {"Lcom/offlinepayment/ui/auth/AuthViewModel$Companion;", "", "()V", "Factory", "Landroidx/lifecycle/ViewModelProvider$Factory;", "getFactory", "()Landroidx/lifecycle/ViewModelProvider$Factory;", "createFactory", "context", "Landroid/content/Context;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.lifecycle.ViewModelProvider.Factory createFactory(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.lifecycle.ViewModelProvider.Factory getFactory() {
            return null;
        }
    }
}