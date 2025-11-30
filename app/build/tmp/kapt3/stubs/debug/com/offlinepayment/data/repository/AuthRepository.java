package com.offlinepayment.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00a0\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B#\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ<\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000e2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u0013\u001a\u00020\u00112\u0006\u0010\u0014\u001a\u00020\u0011H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0015\u0010\u0016J\u001c\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00180\u000eH\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0019\u0010\u001aJ\u0018\u0010\u001b\u001a\u0004\u0018\u00010\u001c2\u0006\u0010\u0010\u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010\u001dJ4\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u001f0\u000e2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010 \u001a\u00020\u00112\u0006\u0010\u0014\u001a\u00020\u0011H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b!\u0010\"J4\u0010#\u001a\b\u0012\u0004\u0012\u00020$0\u000e2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010 \u001a\u00020\u00112\u0006\u0010\u0014\u001a\u00020\u0011H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b%\u0010\"J\u0006\u0010&\u001a\u00020\'J$\u0010(\u001a\b\u0012\u0004\u0012\u00020)0\u000e2\u0006\u0010\u0014\u001a\u00020\u0011H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b*\u0010\u001dJ@\u0010+\u001a\b\u0012\u0004\u0012\u0002H,0\u000e\"\u0004\b\u0000\u0010,2\u001c\u0010-\u001a\u0018\b\u0001\u0012\n\u0012\b\u0012\u0004\u0012\u0002H,0/\u0012\u0006\u0012\u0004\u0018\u00010\u00010.H\u0082@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b0\u00101J@\u00102\u001a\u00020\'2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010 \u001a\u00020\u00112\u0006\u00103\u001a\u00020\u00112\u0006\u00104\u001a\u0002052\u0006\u00106\u001a\u00020\u00112\b\u00107\u001a\u0004\u0018\u00010\u0011H\u0082@\u00a2\u0006\u0002\u00108J$\u00109\u001a\b\u0012\u0004\u0012\u00020:0\u000e2\u0006\u0010;\u001a\u00020<H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b=\u0010>J$\u0010?\u001a\b\u0012\u0004\u0012\u00020@0\u000e2\u0006\u0010;\u001a\u00020AH\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\bB\u0010CJ\u001e\u0010D\u001a\u00020E2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010 \u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010FR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0004\u001a\u0004\u0018\u00010\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006G"}, d2 = {"Lcom/offlinepayment/data/repository/AuthRepository;", "", "api", "Lcom/offlinepayment/data/network/AuthApi;", "context", "Landroid/content/Context;", "ioDispatcher", "Lkotlinx/coroutines/CoroutineDispatcher;", "(Lcom/offlinepayment/data/network/AuthApi;Landroid/content/Context;Lkotlinx/coroutines/CoroutineDispatcher;)V", "database", "Lcom/offlinepayment/data/local/AppDatabase;", "offlineUserDao", "Lcom/offlinepayment/data/local/OfflineUserDao;", "confirmLogin", "Lkotlin/Result;", "Lcom/offlinepayment/data/LoginConfirmResponse;", "email", "", "otp", "nonce", "deviceFingerprint", "confirmLogin-yxL6bBk", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "fetchUserInfo", "Lcom/offlinepayment/data/UserInfoResponse;", "fetchUserInfo-IoAF18A", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getOfflineUser", "Lcom/offlinepayment/data/local/OfflineUser;", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "loginOffline", "Lcom/offlinepayment/data/session/AuthSession;", "password", "loginOffline-BWLJW6A", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "loginStepOne", "Lcom/offlinepayment/data/LoginStep1Response;", "loginStepOne-BWLJW6A", "logout", "", "refreshToken", "Lcom/offlinepayment/data/TokenRefreshResponse;", "refreshToken-gIAlu-s", "safeApiCall", "T", "block", "Lkotlin/Function1;", "Lkotlin/coroutines/Continuation;", "safeApiCall-gIAlu-s", "(Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveUserForOfflineLogin", "verifiedEmail", "userId", "", "name", "phone", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "signup", "Lcom/offlinepayment/data/SignupResponse;", "request", "Lcom/offlinepayment/data/SignupRequest;", "signup-gIAlu-s", "(Lcom/offlinepayment/data/SignupRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "verifyEmail", "Lcom/offlinepayment/data/VerifyEmailResponse;", "Lcom/offlinepayment/data/VerifyEmailRequest;", "verifyEmail-gIAlu-s", "(Lcom/offlinepayment/data/VerifyEmailRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "verifyOfflinePassword", "", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class AuthRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.offlinepayment.data.network.AuthApi api = null;
    @org.jetbrains.annotations.Nullable()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineDispatcher ioDispatcher = null;
    @org.jetbrains.annotations.Nullable()
    private final com.offlinepayment.data.local.AppDatabase database = null;
    @org.jetbrains.annotations.Nullable()
    private final com.offlinepayment.data.local.OfflineUserDao offlineUserDao = null;
    
    public AuthRepository(@org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.network.AuthApi api, @org.jetbrains.annotations.Nullable()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineDispatcher ioDispatcher) {
        super();
    }
    
    public final void logout() {
    }
    
    /**
     * Save verified user to local database for offline login
     */
    private final java.lang.Object saveUserForOfflineLogin(java.lang.String email, java.lang.String password, java.lang.String verifiedEmail, int userId, java.lang.String name, java.lang.String phone, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Check if user exists in local database for offline login
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getOfflineUser(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.offlinepayment.data.local.OfflineUser> $completion) {
        return null;
    }
    
    /**
     * Verify password for offline login
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object verifyOfflinePassword(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
}