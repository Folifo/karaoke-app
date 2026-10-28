package com.karaoke.entitlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.karaoke.common.ApiException;
import com.karaoke.common.ErrorCode;
import com.karaoke.song.Song;
import com.karaoke.subscription.Subscription;
import com.karaoke.subscription.SubscriptionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntitlementServiceTest {

    // 23h30 UTC le 15 janvier = déjà le 16 janvier à Paris.
    private static final Instant NOW = Instant.parse("2026-01-15T23:30:00Z");
    private static final LocalDate PARIS_DAY = LocalDate.of(2026, 1, 16);
    private static final UUID USER = UUID.randomUUID();

    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final UsageRepository usage = mock(UsageRepository.class);
    private final EntitlementService service = new EntitlementService(subscriptions, usage,
            new FreeTierProperties(3, ZoneId.of("Europe/Paris")), Clock.fixed(NOW, ZoneOffset.UTC));

    private final Song freeSong = song(true);
    private final Song premiumSong = song(false);

    @Test
    void subscriberPlaysAnySongWithoutConsumingQuota() {
        givenSubscription(true);

        service.authorizePlay(USER, premiumSong);
        service.authorizePlay(USER, freeSong);

        verify(usage, never()).incrementIfBelow(any(), any(), anyInt());
    }

    @Test
    void freeUserCannotPlayPremiumSong() {
        givenSubscription(false);

        assertThatThrownBy(() -> service.authorizePlay(USER, premiumSong))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.SUBSCRIPTION_REQUIRED);
        verify(usage, never()).incrementIfBelow(any(), any(), anyInt());
    }

    @Test
    void freeUserConsumesQuotaOnTheLocalDay() {
        givenSubscription(false);
        when(usage.incrementIfBelow(USER, PARIS_DAY, 3)).thenReturn(true);

        service.authorizePlay(USER, freeSong);

        verify(usage).incrementIfBelow(USER, PARIS_DAY, 3);
    }

    @Test
    void freeUserIsBlockedOnceQuotaIsReached() {
        givenSubscription(false);
        when(usage.incrementIfBelow(USER, PARIS_DAY, 3)).thenReturn(false);

        assertThatThrownBy(() -> service.authorizePlay(USER, freeSong))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.FREE_LIMIT_REACHED);
    }

    @Test
    void reportsUsageOfTheLocalDay() {
        when(usage.songsPlayed(USER, PARIS_DAY)).thenReturn(2);

        assertThat(service.songsPlayedToday(USER)).isEqualTo(2);
        assertThat(service.dailyLimit()).isEqualTo(3);
    }

    private void givenSubscription(boolean active) {
        Optional<Subscription> subscription = active
                ? Optional.of(new Subscription(USER, Subscription.Platform.APPLE, "monthly", "tx",
                        Subscription.Status.ACTIVE, NOW.plusSeconds(3600)))
                : Optional.empty();
        when(subscriptions.findFirstByUserIdAndStatusAndExpiresAtAfterOrderByExpiresAtDesc(
                USER, Subscription.Status.ACTIVE, NOW)).thenReturn(subscription);
    }

    private static Song song(boolean free) {
        return new Song("Titre", "Artiste", 1000, "key.m4a", "[00:01.00] la", "fr", null, free);
    }
}
