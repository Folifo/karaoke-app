package com.karaoke.common;

import org.springframework.http.HttpStatus;

/** Codes d'erreur stables exposés dans le champ {@code code} des réponses problem+json. */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "La requête contient des champs invalides."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "La requête est illisible ou mal formée."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentification requise."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Session expirée, reconnecte-toi."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Accès refusé."),
    SUBSCRIPTION_REQUIRED(HttpStatus.FORBIDDEN, "Cette chanson est réservée aux abonnés."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Ressource introuvable."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Méthode non autorisée."),
    EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "Cet email est déjà associé à un compte."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Type de contenu non supporté."),
    FREE_LIMIT_REACHED(HttpStatus.TOO_MANY_REQUESTS, "Limite gratuite du jour atteinte."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue.");

    private final HttpStatus status;
    private final String defaultDetail;

    ErrorCode(HttpStatus status, String defaultDetail) {
        this.status = status;
        this.defaultDetail = defaultDetail;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultDetail() {
        return defaultDetail;
    }

    /** Code générique correspondant à un statut HTTP, pour les erreurs levées par le framework. */
    public static ErrorCode fromStatus(int status) {
        return switch (status) {
            case 400 -> MALFORMED_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> INTERNAL_ERROR;
        };
    }
}
