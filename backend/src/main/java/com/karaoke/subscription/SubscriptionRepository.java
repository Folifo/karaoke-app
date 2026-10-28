package com.karaoke.subscription;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /** Abonnement actif non expiré dont l'échéance est la plus lointaine. */
    Optional<Subscription> findFirstByUserIdAndStatusAndExpiresAtAfterOrderByExpiresAtDesc(
            UUID userId, Subscription.Status status, Instant now);
}
