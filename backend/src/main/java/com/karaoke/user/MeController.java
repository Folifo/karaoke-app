package com.karaoke.user;

import com.karaoke.common.ApiException;
import com.karaoke.common.CurrentUser;
import com.karaoke.common.ErrorCode;
import com.karaoke.entitlement.EntitlementService;
import com.karaoke.subscription.Subscription;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    public record MeResponse(UUID id, String email, String displayName, Instant createdAt,
            SubscriptionStatus subscription, FreeQuota freeQuota) {
    }

    public record SubscriptionStatus(boolean active, Subscription.Platform platform, String productId,
            Instant expiresAt) {

        static SubscriptionStatus of(Subscription subscription) {
            return new SubscriptionStatus(true, subscription.getPlatform(), subscription.getProductId(),
                    subscription.getExpiresAt());
        }

        static SubscriptionStatus inactive() {
            return new SubscriptionStatus(false, null, null, null);
        }
    }

    public record FreeQuota(int dailyLimit, int usedToday) {
    }

    private final UserRepository users;
    private final EntitlementService entitlements;

    public MeController(UserRepository users, EntitlementService entitlements) {
        this.users = users;
        this.entitlements = entitlements;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = CurrentUser.id(jwt);
        // Compte supprimé alors que l'access token est encore valable.
        User user = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        SubscriptionStatus subscription = entitlements.activeSubscription(userId)
                .map(SubscriptionStatus::of)
                .orElseGet(SubscriptionStatus::inactive);
        return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt(),
                subscription, new FreeQuota(entitlements.dailyLimit(), entitlements.songsPlayedToday(userId)));
    }
}
