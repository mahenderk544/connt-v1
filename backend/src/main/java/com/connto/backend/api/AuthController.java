package com.connto.backend.api;

import com.connto.backend.domain.OtpPurpose;
import com.connto.backend.service.AuthService;
import com.connto.backend.service.AuthService.AuthResult;
import com.connto.backend.service.OtpService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    /**
     * Step 1: request a 6-digit OTP for REGISTER or LOGIN. Integrate SMS in {@link
     * com.connto.backend.notify.OtpDeliveryService} for production.
     */
    @PostMapping("/otp/request")
    public OtpRequestResponse requestOtp(@RequestBody @jakarta.validation.Valid OtpRequest body) {
        String devCode = otpService.requestOtp(body.phone(), body.purpose());
        String msg =
                body.purpose() == OtpPurpose.REGISTER
                        ? "Verification code sent for registration"
                        : "Verification code sent for sign-in";
        return new OtpRequestResponse(msg, devCode);
    }

    /** Step 2 (register): create account after OTP verified. Password optional (random one set if omitted). */
    @PostMapping("/register")
    public AuthResponse register(@RequestBody @jakarta.validation.Valid RegisterRequest body) {
        AuthResult r =
                authService.registerWithOtp(
                        body.phone(), body.otp(), body.displayName(), body.password());
        return new AuthResponse(r.token(), r.userId().toString());
    }

    /** Step 2 (login): issue JWT after OTP verified. */
    @PostMapping("/login")
    public AuthResponse login(@RequestBody @jakarta.validation.Valid LoginRequest body) {
        AuthResult r = authService.loginWithOtp(body.phone(), body.otp());
        return new AuthResponse(r.token(), r.userId().toString());
    }

    public record OtpRequest(
            @NotBlank @Size(max = 32) String phone, @NotNull OtpPurpose purpose) {}

    public record OtpRequestResponse(String message, String devCode) {}

    public record RegisterRequest(
            @NotBlank @Size(max = 32) String phone,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits") String otp,
            @Size(max = 120) String displayName,
            @Size(min = 8, max = 128) String password) {}

    public record LoginRequest(
            @NotBlank @Size(max = 32) String phone,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
                    String otp) {}

    public record AuthResponse(String token, String userId) {}
}
