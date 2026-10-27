package com.karaoke.entitlement;

import com.karaoke.common.ApiException;
import com.karaoke.common.ErrorCode;
import com.karaoke.song.Song;
import com.karaoke.subscription.Subscription;
import com.karaoke.subscription.SubscriptionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Droits d'accès aux chansons. Le backend est la source de vérité :
 * un abonné actif a accès à tout le catalogue ; un utilisateur gratuit n'a accès qu'aux
 * chansons gratuites, dans la limite d'un nombre de lectures par jour.
 */
@Service
public class EntitlementService {

    private final SubscriptionRepository subscriptions;
    private final UsageRepository usage;
    private final FreeTierProperties freeTier;
    private final Clock clock;

    public EntitlementService(SubscriptionRepository subscriptions, UsageRepository usage,
            FreeTierProperties freeTier, Clock clock) {
        this.subscriptions = subscriptions;
        this.usage = usage;
        this.freeTier = freeTier;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> activeSubscription(UUID userId) {
        return subscriptions.findFirstByUserIdAndStatusAndExpiresAtAfterOrderByExpiresAtDesc(
                userId, Subscription.Status.ACTIVE, clock.instant());
    }

    /** Vérifie que l'utilisateur peut jouer la chanson et, s'il est gratuit, décompte une lecture. */
    @Transactional
    public void authorizePlay(UUID userId, Song song) {
        if (activeSubscription(userId).isPresent()) {
            return;
        }
        if (!song.isFree()) {
            throw new ApiException(ErrorCode.SUBSCRIPTION_REQUIRED);
        }
        if (!usage.incrementIfBelow(userId, today(), freeTier.dailySongLimit())) {
            throw new ApiException(ErrorCode.FREE_LIMIT_REACHED,
                    "Limite de " + freeTier.dailySongLimit() + " chansons gratuites par jour atteinte.");
        }
    }

    public int dailyLimit() {
        return freeTier.dailySongLimit();
    }

    @Transactional(readOnly = true)
    public int songsPlayedToday(UUID userId) {
        return usage.songsPlayed(userId, today());
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(freeTier.zone()));
    }
}
