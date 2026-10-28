package com.karaoke.common;

import org.springframework.http.ProblemDetail;

public final class Problems {

    public static final String CODE = "code";

    private Problems() {
    }

    public static ProblemDetail of(ErrorCode code) {
        return of(code, code.defaultDetail());
    }

    public static ProblemDetail of(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setProperty(CODE, code.name());
        return problem;
    }
}
