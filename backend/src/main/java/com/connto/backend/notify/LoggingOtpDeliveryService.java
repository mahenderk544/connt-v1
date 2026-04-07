package com.connto.backend.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Stub implementation: logs that an OTP was issued. Replace with SNS / Twilio / etc. for production.
 */
@Service
@Primary
public class LoggingOtpDeliveryService implements OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpDeliveryService.class);

    @Override
    public void sendOtp(String normalizedPhone, String sixDigitCode) {
        String tail = normalizedPhone.length() > 4 ? normalizedPhone.substring(normalizedPhone.length() - 4) : "****";
        log.info("OTP delivery stub: phone ends with {}, code logged at DEBUG", tail);
        log.debug("OTP code for {} is {}", normalizedPhone, sixDigitCode);
    }
}
