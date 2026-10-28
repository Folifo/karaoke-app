package com.karaoke.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UpdateTimestamp;

/** Abonnement Apple/Google. Alimenté par la vérification des achats (étape 3). */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    public enum Platform { APPLE, GOOGLE }

    public enum Status { ACTIVE, EXPIRED, CANCELED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "original_transaction_id", nullable = false)
    private String originalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Subscription() {
    }

    public Subscription(UUID userId, Platform platform, String productId, String originalTransactionId,
            Status status, Instant expiresAt) {
        this.userId = userId;
        this.platform = platform;
        this.productId = productId;
        this.originalTransactionId = originalTransactionId;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getProductId() {
        return productId;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
