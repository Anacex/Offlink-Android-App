package com.offlinepayment.data.network;

/**
 * Retrofit contract for FastAPI auth routes.
 * NOTE: Response fields are based on current backend implementation and may change.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0007\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\tJ\u0018\u0010\n\u001a\u00020\u000b2\b\b\u0001\u0010\u0004\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u0018\u0010\u000e\u001a\u00020\u000f2\b\b\u0001\u0010\u0004\u001a\u00020\u0010H\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u0018\u0010\u0012\u001a\u00020\u00132\b\b\u0001\u0010\u0004\u001a\u00020\u0014H\u00a7@\u00a2\u0006\u0002\u0010\u0015J\u0018\u0010\u0016\u001a\u00020\u00172\b\b\u0001\u0010\u0004\u001a\u00020\u0018H\u00a7@\u00a2\u0006\u0002\u0010\u0019\u00a8\u0006\u001a"}, d2 = {"Lcom/offlinepayment/data/network/AuthApi;", "", "confirmLogin", "Lcom/offlinepayment/data/LoginConfirmResponse;", "request", "Lcom/offlinepayment/data/LoginConfirmRequest;", "(Lcom/offlinepayment/data/LoginConfirmRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getUserInfo", "Lcom/offlinepayment/data/UserInfoResponse;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "login", "Lcom/offlinepayment/data/LoginStep1Response;", "Lcom/offlinepayment/data/LoginRequest;", "(Lcom/offlinepayment/data/LoginRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refreshToken", "Lcom/offlinepayment/data/TokenRefreshResponse;", "Lcom/offlinepayment/data/RefreshTokenRequest;", "(Lcom/offlinepayment/data/RefreshTokenRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "signup", "Lcom/offlinepayment/data/SignupResponse;", "Lcom/offlinepayment/data/SignupRequest;", "(Lcom/offlinepayment/data/SignupRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "verifyEmail", "Lcom/offlinepayment/data/VerifyEmailResponse;", "Lcom/offlinepayment/data/VerifyEmailRequest;", "(Lcom/offlinepayment/data/VerifyEmailRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface AuthApi {
    
    @retrofit2.http.POST(value = "auth/signup")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object signup(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.SignupRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.offlinepayment.data.SignupResponse> $completion);
    
    @retrofit2.http.POST(value = "auth/verify-email")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object verifyEmail(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.VerifyEmailRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.offlinepayment.data.VerifyEmailResponse> $completion);
    
    @retrofit2.http.POST(value = "auth/login")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object login(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.LoginRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.offlinepayment.data.LoginStep1Response> $completion);
    
    @retrofit2.http.POST(value = "auth/login/confirm")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object confirmLogin(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.LoginConfirmRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.offlinepayment.data.LoginConfirmResponse> $completion);
    
    @retrofit2.http.POST(value = "auth/token/refresh")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object refreshToken(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.offlinepayment.data.RefreshTokenRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.offlinepayment.data.TokenRefreshResponse> $completion);
    
    @retrofit2.http.GET(value = "auth/me")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getUserInfo(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.offlinepayment.data.UserInfoResponse> $completion);
}