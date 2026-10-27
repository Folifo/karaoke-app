package com.karaoke.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class Utf8MaxBytesTest {

    record Form(@Utf8MaxBytes(4) String value) {
    }

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void countsUtf8BytesNotCharacters() {
        assertThat(validator.validate(new Form("abcd"))).isEmpty();
        assertThat(validator.validate(new Form("éé"))).isEmpty();
        assertThat(validator.validate(new Form("ééa"))).hasSize(1);
        assertThat(validator.validate(new Form(null))).isEmpty();
    }
}
