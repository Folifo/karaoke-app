package com.karaoke.storage;

import java.time.Instant;

public record SignedUrl(String url, Instant expiresAt) {
}
