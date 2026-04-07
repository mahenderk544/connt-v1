package com.connto.backend.repository;

import com.connto.backend.domain.OtpPurpose;
import com.connto.backend.domain.PhoneOtpChallenge;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneOtpChallengeRepository extends JpaRepository<PhoneOtpChallenge, UUID> {

    void deleteByPhoneAndPurpose(String phone, OtpPurpose purpose);

    Optional<PhoneOtpChallenge> findFirstByPhoneAndPurposeOrderByCreatedAtDesc(
            String phone, OtpPurpose purpose);
}
