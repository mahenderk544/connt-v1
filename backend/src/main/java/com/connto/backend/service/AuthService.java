package com.connto.backend.service;

import com.connto.backend.domain.AppUser;
import com.connto.backend.domain.OtpPurpose;
import com.connto.backend.domain.Profile;
import com.connto.backend.repository.AppUserRepository;
import com.connto.backend.repository.ProfileRepository;
import com.connto.backend.security.JwtService;
import com.connto.backend.util.PhoneNormalizer;
import com.connto.backend.web.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    public AuthService(
            AppUserRepository users,
            ProfileRepository profiles,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    @Transactional
    public AuthResult registerWithOtp(
            String phoneRaw, String otp, String displayName, String passwordOptional) {
        final String normalized;
        try {
            normalized = PhoneNormalizer.normalize(phoneRaw);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        otpService.verifyAndConsume(normalized, OtpPurpose.REGISTER, otp);

        if (users.existsByPhone(normalized)) {
            throw new ApiException(HttpStatus.CONFLICT, "This mobile number is already registered");
        }

        String pwd = passwordOptional == null ? "" : passwordOptional.trim();
        String secret = !pwd.isEmpty() ? pwd : UUID.randomUUID().toString();

        AppUser user = new AppUser();
        user.setPhone(normalized);
        user.setPasswordHash(passwordEncoder.encode(secret));
        users.save(user);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setDisplayName(
                displayName != null && !displayName.isBlank() ? displayName : normalized);
        profiles.save(profile);

        String token = jwtService.createToken(user.getId());
        return new AuthResult(token, user.getId());
    }

    @Transactional
    public AuthResult loginWithOtp(String phoneRaw, String otp) {
        final String normalized;
        try {
            normalized = PhoneNormalizer.normalize(phoneRaw);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        otpService.verifyAndConsume(normalized, OtpPurpose.LOGIN, otp);

        AppUser user =
                users.findByPhone(normalized)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "No account for this number"));

        String token = jwtService.createToken(user.getId());
        return new AuthResult(token, user.getId());
    }

    public record AuthResult(String token, UUID userId) {}
}
