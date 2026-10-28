package com.karaoke.common;

/** Erreur métier traduite en réponse problem+json par {@link GlobalExceptionHandler}. */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        this(code, code.defaultDetail());
    }

    public ApiException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
