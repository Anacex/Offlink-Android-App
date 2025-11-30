package com.offlinepayment.data.session;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0006\u001a\u0004\u0018\u00010\u0005J\u000e\u0010\u0007\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00050\bJ\u0010\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\u0005R\u0016\u0010\u0003\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/offlinepayment/data/session/AuthSessionManager;", "", "()V", "sessionFlow", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/offlinepayment/data/session/AuthSession;", "currentSession", "observeSession", "Lkotlinx/coroutines/flow/StateFlow;", "updateSession", "", "newSession", "app_debug"})
public final class AuthSessionManager {
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.MutableStateFlow<com.offlinepayment.data.session.AuthSession> sessionFlow = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.offlinepayment.data.session.AuthSessionManager INSTANCE = null;
    
    private AuthSessionManager() {
        super();
    }
    
    public final void updateSession(@org.jetbrains.annotations.Nullable()
    com.offlinepayment.data.session.AuthSession newSession) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.offlinepayment.data.session.AuthSession currentSession() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.offlinepayment.data.session.AuthSession> observeSession() {
        return null;
    }
}