package com.connto.backend.notify;

public interface OtpDeliveryService {

    void sendOtp(String normalizedPhone, String sixDigitCode);
}
