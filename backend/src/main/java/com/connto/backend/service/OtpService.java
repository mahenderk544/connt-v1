package com.connto.backend.service;

import com.connto.backend.config.OtpProperties;
import com.connto.backend.domain.OtpPurpose;
import com.connto.backend.domain.PhoneOtpChallenge;
import com.connto.backend.notify.OtpDeliveryService;
import com.connto.backend.repository.AppUserRepository;
import com.connto.backend.repository.PhoneOtpChallengeRepository;
import com.connto.backend.util.PhoneNormalizer;
import com.connto.backend.web.ApiException;
import java.security.SecureRandom;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PhoneOtpChallengeRepository challenges;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService delivery;
    private final OtpProperties otpProperties;

    public OtpService(
            PhoneOtpChallengeRepository challenges,
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            OtpDeliveryService delivery,
            OtpProperties otpProperties) {
        this.challenges = challenges;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.delivery = delivery;
        this.otpProperties = otpProperties;
    }

    @Transactional
    public String requestOtp(String phoneRaw, OtpPurpose purpose) {
        final String phone;
        try {
            phone = PhoneNormalizer.normalize(phoneRaw);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        if (purpose == OtpPurpose.REGISTER) {
            if (users.existsByPhone(phone)) {
                throw new ApiException(HttpStatus.CONFLICT, "This number is already registered");
            }
        } else {
            if (!users.existsByPhone(phone)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No account for this number");
            }
        }

        challenges.deleteByPhoneAndPurpose(phone, purpose);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        PhoneOtpChallenge row = new PhoneOtpChallenge();
        row.setPhone(phone);
        row.setPurpose(purpose);
        row.setCodeHash(passwordEncoder.encode(code));
        row.setExpiresAt(Instant.now().plusSeconds(otpProperties.ttlSeconds()));
        challenges.save(row);

        delivery.sendOtp(phone, code);
        return otpProperties.devReturnCode() ? code : null;
    }

    @Transactional
    public void verifyAndConsume(String phoneRaw, OtpPurpose purpose, String otpCode) {
        final String phone;
        try {
            phone = PhoneNormalizer.normalize(phoneRaw);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        if (otpCode == null || !otpCode.matches("\\d{6}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter the 6-digit code");
        }

        PhoneOtpChallenge row =
                challenges
                        .findFirstByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.BAD_REQUEST,
                                                "Request a code first"));

        if (Instant.now().isAfter(row.getExpiresAt())) {
            challenges.delete(row);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Code expired — request a new one");
        }

        if (row.getAttempts() >= otpProperties.maxAttempts()) {
            challenges.delete(row);
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many attempts — request a new code");
        }

        if (!passwordEncoder.matches(otpCode, row.getCodeHash())) {
            row.setAttempts(row.getAttempts() + 1);
            challenges.save(row);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid code");
        }

        challenges.delete(row);
    }
}
