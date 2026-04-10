package com.connto.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connto.wallet")
public class WalletProperties {

    /**
     * MOCK, UPI (NPCI deep link), RAZORPAY, STRIPE. UPI opens Google Pay / PhonePe / Paytm etc. on the device.
     */
    private String paymentProvider = "UPI";

    /** When true, POST /me/wallet/topup/complete-mock finalizes MOCK checkouts. */
    private boolean mockCompleteEnabled = true;

    /**
     * Merchant UPI ID (e.g. connto@paytm). Required when payment-provider is UPI.
     */
    private String upiVpa = "";

    /** Shown in the payer's UPI app. */
    private String upiPayeeName = "Connto";

    /**
     * When true, user can call complete-upi after paying in UPI app (no bank webhook). Disable in production when
     * Razorpay / bank verification is wired.
     */
    private boolean upiClientConfirmEnabled = true;

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public boolean isMockCompleteEnabled() {
        return mockCompleteEnabled;
    }

    public void setMockCompleteEnabled(boolean mockCompleteEnabled) {
        this.mockCompleteEnabled = mockCompleteEnabled;
    }

    public String getUpiVpa() {
        return upiVpa;
    }

    public void setUpiVpa(String upiVpa) {
        this.upiVpa = upiVpa;
    }

    public String getUpiPayeeName() {
        return upiPayeeName;
    }

    public void setUpiPayeeName(String upiPayeeName) {
        this.upiPayeeName = upiPayeeName;
    }

    public boolean isUpiClientConfirmEnabled() {
        return upiClientConfirmEnabled;
    }

    public void setUpiClientConfirmEnabled(boolean upiClientConfirmEnabled) {
        this.upiClientConfirmEnabled = upiClientConfirmEnabled;
    }
}
