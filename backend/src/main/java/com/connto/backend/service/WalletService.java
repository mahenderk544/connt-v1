package com.connto.backend.service;

import com.connto.backend.config.WalletProperties;
import com.connto.backend.domain.AppUser;
import com.connto.backend.domain.WalletLedgerEntry;
import com.connto.backend.domain.WalletTopupOffer;
import com.connto.backend.domain.WalletTopupSession;
import com.connto.backend.domain.WalletTopupSessionStatus;
import com.connto.backend.repository.AppUserRepository;
import com.connto.backend.repository.WalletLedgerEntryRepository;
import com.connto.backend.repository.WalletTopupOfferRepository;
import com.connto.backend.repository.WalletTopupSessionRepository;
import com.connto.backend.web.ApiException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    public static final String ENTRY_TOPUP = "TOPUP";

    private final WalletTopupOfferRepository offers;
    private final WalletTopupSessionRepository sessions;
    private final WalletLedgerEntryRepository ledger;
    private final AppUserRepository users;
    private final WalletProperties walletProperties;

    public WalletService(
            WalletTopupOfferRepository offers,
            WalletTopupSessionRepository sessions,
            WalletLedgerEntryRepository ledger,
            AppUserRepository users,
            WalletProperties walletProperties) {
        this.offers = offers;
        this.sessions = sessions;
        this.ledger = ledger;
        this.users = users;
        this.walletProperties = walletProperties;
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listActiveOffers() {
        return offers.findByActiveTrueOrderBySortOrderAscAmountPaiseAsc().stream()
                .map(OfferResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalance(UUID userId) {
        AppUser u =
                users.findById(userId)
                        .orElseThrow(
                                () -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return new WalletBalanceResponse(u.getWalletBalancePaise());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactions(UUID userId) {
        return ledger.findTop100ByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TopupInitiateResponse initiateTopup(UUID userId, UUID offerId) {
        WalletTopupOffer offer =
                offers.findById(offerId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));
        if (!offer.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Offer is not available");
        }
        String provider = walletProperties.getPaymentProvider().trim().toUpperCase(Locale.ROOT);
        if (provider.isEmpty()) {
            provider = "MOCK";
        }

        if ("UPI".equals(provider)) {
            String vpa = walletProperties.getUpiVpa();
            if (vpa == null || vpa.isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "UPI is enabled but connto.wallet.upi-vpa is not set (your merchant UPI ID)");
            }
        }

        WalletTopupSession s = new WalletTopupSession();
        s.setId(UUID.randomUUID());
        s.setUser(users.getReferenceById(userId));
        s.setOffer(offer);
        s.setAmountPaise(offer.getAmountPaise());
        s.setTalkMinutes(offer.getTalkMinutes());
        s.setStatus(WalletTopupSessionStatus.PENDING);
        s.setPaymentProvider(provider);
        if ("RAZORPAY".equals(provider)) {
            s.setExternalOrderId("pending_razorpay_order");
        } else if ("STRIPE".equals(provider)) {
            s.setExternalOrderId("pending_stripe_payment_intent");
        }
        sessions.save(s);

        String upiDeepLink = null;
        boolean upiConfirmAllowed = false;
        if ("UPI".equals(provider)) {
            String note =
                    String.format(
                            Locale.US,
                            "Connto wallet ₹%.2f",
                            offer.getAmountPaise() / 100.0);
            upiDeepLink =
                    buildUpiDeepLink(
                            walletProperties.getUpiVpa().trim(),
                            walletProperties.getUpiPayeeName() != null
                                    ? walletProperties.getUpiPayeeName()
                                    : "Connto",
                            offer.getAmountPaise(),
                            s.getId(),
                            note);
            upiConfirmAllowed = walletProperties.isUpiClientConfirmEnabled();
        }

        String gatewayHint =
                switch (provider) {
                    case "MOCK" -> "Use the simulated pay button when mock payments are enabled.";
                    case "UPI" ->
                            upiConfirmAllowed
                                    ? "Pay in your UPI app, then tap I've paid here so your wallet updates (dev / MVP). "
                                            + "For production, disable upi-client-confirm and verify via Razorpay or bank."
                                    : "Complete payment in your UPI app. Wallet credit requires a server webhook when "
                                            + "client confirm is disabled.";
                    case "RAZORPAY" ->
                            "Use Razorpay Checkout with UPI; confirm server-side and mark the session paid.";
                    case "STRIPE" ->
                            "Use Stripe PaymentElement; confirm server-side and mark the session paid.";
                    default -> "Configure connto.wallet.payment-provider.";
                };

        return new TopupInitiateResponse(
                s.getId(),
                provider,
                offer.getAmountPaise(),
                offer.getTalkMinutes(),
                offer.getLabel(),
                "RAZORPAY".equals(provider) ? s.getExternalOrderId() : null,
                gatewayHint,
                upiDeepLink,
                upiConfirmAllowed);
    }

    /**
     * NPCI UPI deep link — opens Google Pay, PhonePe, Paytm, etc. on the phone.
     */
    static String buildUpiDeepLink(
            String vpa, String payeeName, int amountPaise, UUID transactionRef, String note) {
        String am = String.format(Locale.US, "%.2f", amountPaise / 100.0);
        // Keep am/cu unencoded — some UPI apps are strict about amount format.
        return "upi://pay?pa="
                + url(vpa)
                + "&pn="
                + url(payeeName)
                + "&am="
                + am
                + "&cu=INR&tr="
                + url(transactionRef.toString())
                + "&tn="
                + url(note);
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Transactional
    public WalletBalanceResponse completeMockTopup(UUID userId, UUID sessionId) {
        if (!walletProperties.isMockCompleteEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Mock top-up completion is disabled");
        }
        WalletTopupSession s =
                sessions
                        .findByIdAndUserId(sessionId, userId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));
        if (s.getStatus() != WalletTopupSessionStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Session is not pending");
        }
        if (!"MOCK".equalsIgnoreCase(s.getPaymentProvider())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "complete-mock is only for MOCK sessions; got " + s.getPaymentProvider());
        }
        return finalizeTopup(s, userId);
    }

    @Transactional
    public WalletBalanceResponse completeUpiTopup(UUID userId, UUID sessionId) {
        if (!walletProperties.isUpiClientConfirmEnabled()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "UPI client confirm is disabled — use a payment gateway webhook to credit the wallet");
        }
        WalletTopupSession s =
                sessions
                        .findByIdAndUserId(sessionId, userId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));
        if (s.getStatus() != WalletTopupSessionStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Session is not pending");
        }
        if (!"UPI".equalsIgnoreCase(s.getPaymentProvider())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "complete-upi is only for UPI sessions; got " + s.getPaymentProvider());
        }
        return finalizeTopup(s, userId);
    }

    private WalletBalanceResponse finalizeTopup(WalletTopupSession s, UUID userId) {
        AppUser u =
                users.findById(userId)
                        .orElseThrow(
                                () -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        int credit = s.getAmountPaise();
        u.setWalletBalancePaise(u.getWalletBalancePaise() + credit);
        users.save(u);

        WalletLedgerEntry e = new WalletLedgerEntry();
        e.setId(UUID.randomUUID());
        e.setUser(u);
        e.setEntryType(ENTRY_TOPUP);
        e.setAmountPaise(credit);
        e.setLabel(buildTopupLabel(credit, s.getTalkMinutes()));
        e.setOffer(s.getOffer());
        e.setSession(s);
        ledger.save(e);

        s.setStatus(WalletTopupSessionStatus.COMPLETED);
        s.setCompletedAt(Instant.now());
        sessions.save(s);

        return new WalletBalanceResponse(u.getWalletBalancePaise());
    }

    private static String buildTopupLabel(int amountPaise, int talkMinutes) {
        double rs = amountPaise / 100.0;
        return String.format(Locale.US, "Added ₹%.2f · %d min talk credit", rs, talkMinutes);
    }

    public record OfferResponse(
            UUID id, int amountPaise, int talkMinutes, String label, int sortOrder) {
        static OfferResponse from(WalletTopupOffer o) {
            return new OfferResponse(
                    o.getId(), o.getAmountPaise(), o.getTalkMinutes(), o.getLabel(), o.getSortOrder());
        }
    }

    public record WalletBalanceResponse(long balancePaise) {}

    public record TransactionResponse(
            UUID id, String type, long amountPaise, String label, Instant createdAt) {
        static TransactionResponse from(WalletLedgerEntry e) {
            return new TransactionResponse(
                    e.getId(),
                    e.getEntryType(),
                    e.getAmountPaise(),
                    e.getLabel(),
                    e.getCreatedAt());
        }
    }

    public record TopupInitiateResponse(
            UUID sessionId,
            String paymentProvider,
            int amountPaise,
            int talkMinutes,
            String offerLabel,
            String razorpayOrderId,
            String clientMessage,
            String upiDeepLink,
            boolean upiConfirmAllowed) {}
}
