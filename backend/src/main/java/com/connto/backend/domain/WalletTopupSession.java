package com.connto.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_topup_sessions")
public class WalletTopupSession {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id")
    private WalletTopupOffer offer;

    @Column(name = "amount_paise", nullable = false)
    private int amountPaise;

    @Column(name = "talk_minutes", nullable = false)
    private int talkMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private WalletTopupSessionStatus status = WalletTopupSessionStatus.PENDING;

    @Column(name = "payment_provider", nullable = false, length = 32)
    private String paymentProvider;

    @Column(name = "external_order_id", length = 255)
    private String externalOrderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public WalletTopupOffer getOffer() {
        return offer;
    }

    public void setOffer(WalletTopupOffer offer) {
        this.offer = offer;
    }

    public int getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(int amountPaise) {
        this.amountPaise = amountPaise;
    }

    public int getTalkMinutes() {
        return talkMinutes;
    }

    public void setTalkMinutes(int talkMinutes) {
        this.talkMinutes = talkMinutes;
    }

    public WalletTopupSessionStatus getStatus() {
        return status;
    }

    public void setStatus(WalletTopupSessionStatus status) {
        this.status = status;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
