package com.tradex.services;

import com.tradex.exceptions.ValidationException;
import com.tradex.utils.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    @Test void hashVerifiesCorrectPasswordOnly() {
        String h = PasswordHasher.hash("s3cret-pass");
        assertTrue(PasswordHasher.verify("s3cret-pass", h));
        assertFalse(PasswordHasher.verify("wrong", h));
    }

    @Test void hashesAreSalted() {
        assertNotEquals(PasswordHasher.hash("same-password"), PasswordHasher.hash("same-password"));
    }

    @Test void rejectsMalformedStoredHash() {
        assertFalse(PasswordHasher.verify("x", "garbage"));
    }

    @Test void validationRejectsBadInput() {
        assertThrows(ValidationException.class, () -> AuthService.validate("ab", "a@b.co", "longenough"));
        assertThrows(ValidationException.class, () -> AuthService.validate("good_name", "not-an-email", "longenough"));
        assertThrows(ValidationException.class, () -> AuthService.validate("good_name", "a@b.co", "short"));
        assertDoesNotThrow(() -> AuthService.validate("good_name", "a@b.co", "longenough"));
    }
}
