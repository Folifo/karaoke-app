package com.karaoke.auth;

import com.karaoke.auth.AuthDtos.LoginRequest;
import com.karaoke.auth.AuthDtos.RegisterRequest;
import com.karaoke.auth.AuthDtos.TokenResponse;
import com.karaoke.common.ApiException;
import com.karaoke.common.ErrorCode;
import com.karaoke.user.User;
import com.karaoke.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    /** Comparé quand l'email est inconnu, pour que la durée de réponse ne révèle pas les comptes existants. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, TokenService tokenService,
            PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_USED);
        }
        User user;
        try {
            user = users.saveAndFlush(new User(email, passwordEncoder.encode(request.password()),
                    request.displayName()));
        } catch (DataIntegrityViolationException e) {
            // Inscription concurrente avec le même email.
            throw new ApiException(ErrorCode.EMAIL_ALREADY_USED);
        }
        return tokenService.issue(user.getId());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Optional<User> user = users.findByEmail(normalizeEmail(request.email()));
        // Au-delà de 72 octets, BCrypt refuse le mot de passe : il ne peut correspondre à aucun compte.
        boolean comparable = request.password().getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
        String password = comparable ? request.password() : "";
        boolean matches = passwordEncoder.matches(password,
                user.map(User::getPasswordHash).orElse(dummyPasswordHash));
        if (user.isEmpty() || !comparable || !matches) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        return tokenService.issue(user.get().getId());
    }

    /**
     * Rotation : le refresh token présenté est révoqué et remplacé. Un token déjà révoqué qui
     * revient signale un vol probable : toutes les sessions de l'utilisateur sont fermées.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse refresh(String refreshToken) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokens.findByTokenHash(TokenService.hash(refreshToken))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (!token.getExpiresAt().isAfter(now)) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (token.getRevokedAt() != null || refreshTokens.revoke(token.getId(), now) == 0) {
            log.warn("Réutilisation d'un refresh token révoqué pour l'utilisateur {} : sessions révoquées",
                    token.getUserId());
            refreshTokens.revokeAllForUser(token.getUserId(), now);
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return tokenService.issue(token.getUserId());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokens.findByTokenHash(TokenService.hash(refreshToken))
                .ifPresent(token -> refreshTokens.revoke(token.getId(), clock.instant()));
    }

    static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
