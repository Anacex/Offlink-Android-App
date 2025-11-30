package com.offlinepayment.ui.auth;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000R\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\u00b8\u0001\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032`\u0010\u0004\u001a\\\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\b\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\t\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\n\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\u000b\u0012\u0004\u0012\u00020\u00010\u000526\u0010\f\u001a2\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\t\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\u000e\u0012\u0004\u0012\u00020\u00010\r2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u0010H\u0007\u001a^\u0010\u0011\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\u00062\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00132\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u00102\u0006\u0010\u0015\u001a\u00020\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u00062\b\u0010\u0018\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0019\u001a\u00020\u001aH\u0003\u001a\u00fe\u0001\u0010\u001b\u001a\u00020\u00012\u0006\u0010\u001c\u001a\u00020\u00062\u0012\u0010\u001d\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00132\u0006\u0010\t\u001a\u00020\u00062\u0012\u0010\u001e\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00132\u0006\u0010\u001f\u001a\u00020\u00062\u0012\u0010 \u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00132\u0006\u0010\n\u001a\u00020\u00062\u0012\u0010!\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00132\u0006\u0010\"\u001a\u00020\u00062\u0012\u0010#\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00132\u0006\u0010$\u001a\u00020\u00162\f\u0010%\u001a\b\u0012\u0004\u0012\u00020\u00010\u00102\u0006\u0010&\u001a\u00020\u00162\f\u0010\'\u001a\b\u0012\u0004\u0012\u00020\u00010\u00102\u0006\u0010(\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00062\b\u0010\u0018\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0015\u001a\u00020\u00162\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00102\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u0010H\u0003\u001a\"\u0010)\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00160+0*2\u0006\u0010\n\u001a\u00020\u0006H\u0002\u001a\u0010\u0010,\u001a\u00020\u00162\u0006\u0010\n\u001a\u00020\u0006H\u0002\u00a8\u0006-"}, d2 = {"CreateAccountScreen", "", "uiState", "Lcom/offlinepayment/ui/auth/AuthUiState;", "onSignup", "Lkotlin/Function4;", "", "Lkotlin/ParameterName;", "name", "email", "password", "phone", "onVerifyEmail", "Lkotlin/Function2;", "otp", "onBackToLogin", "Lkotlin/Function0;", "OtpVerificationSection", "onOtpChange", "Lkotlin/Function1;", "onVerify", "isLoading", "", "errorMessage", "successMessage", "otpFocusRequester", "Landroidx/compose/ui/focus/FocusRequester;", "RegistrationFormSection", "fullName", "onFullNameChange", "onEmailChange", "phoneNumber", "onPhoneNumberChange", "onPasswordChange", "confirmPassword", "onConfirmPasswordChange", "passwordVisible", "onPasswordVisibleToggle", "confirmPasswordVisible", "onConfirmPasswordVisibleToggle", "showPasswordRequirements", "getPasswordRequirements", "", "Lkotlin/Pair;", "isPasswordValid", "app_debug"})
public final class CreateAccountScreenKt {
    
    /**
     * Validates password complexity to match server requirements:
     * - At least 10 characters
     * - Contains uppercase letter
     * - Contains lowercase letter
     * - Contains a digit
     * - Contains a special character
     */
    private static final boolean isPasswordValid(java.lang.String password) {
        return false;
    }
    
    /**
     * Returns detailed password requirement status
     */
    private static final java.util.List<kotlin.Pair<java.lang.String, java.lang.Boolean>> getPasswordRequirements(java.lang.String password) {
        return null;
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void CreateAccountScreen(@org.jetbrains.annotations.NotNull()
    com.offlinepayment.ui.auth.AuthUiState uiState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function4<? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, kotlin.Unit> onSignup, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.lang.String, ? super java.lang.String, kotlin.Unit> onVerifyEmail, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBackToLogin) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void OtpVerificationSection(java.lang.String email, java.lang.String otp, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onOtpChange, kotlin.jvm.functions.Function0<kotlin.Unit> onVerify, boolean isLoading, java.lang.String errorMessage, java.lang.String successMessage, androidx.compose.ui.focus.FocusRequester otpFocusRequester) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void RegistrationFormSection(java.lang.String fullName, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onFullNameChange, java.lang.String email, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onEmailChange, java.lang.String phoneNumber, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onPhoneNumberChange, java.lang.String password, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onPasswordChange, java.lang.String confirmPassword, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onConfirmPasswordChange, boolean passwordVisible, kotlin.jvm.functions.Function0<kotlin.Unit> onPasswordVisibleToggle, boolean confirmPasswordVisible, kotlin.jvm.functions.Function0<kotlin.Unit> onConfirmPasswordVisibleToggle, boolean showPasswordRequirements, java.lang.String errorMessage, java.lang.String successMessage, boolean isLoading, kotlin.jvm.functions.Function0<kotlin.Unit> onSignup, kotlin.jvm.functions.Function0<kotlin.Unit> onBackToLogin) {
    }
}