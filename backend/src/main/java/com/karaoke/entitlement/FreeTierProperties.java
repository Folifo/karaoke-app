package com.karaoke.entitlement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("karaoke.free-tier")
public record FreeTierProperties(@PositiveOrZero int dailySongLimit, @NotNull ZoneId zone) {
}
