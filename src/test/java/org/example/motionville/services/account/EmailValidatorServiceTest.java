package org.example.motionville.services.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorServiceTest {

    private EmailValidatorService validator;

    @BeforeEach
    void setUp() {
        validator = new EmailValidatorService(true);
    }

    @Test
    void acceptsReservedTestDomainsWithoutDnsCall() {
        assertDoesNotThrow(() -> validator.validateEmailDomainMx("user@example.com"));
        assertDoesNotThrow(() -> validator.validateEmailDomainMx("user@test.example"));
        assertDoesNotThrow(() -> validator.validateEmailDomainMx("user@sub.test"));
        assertDoesNotThrow(() -> validator.validateEmailDomainMx("admin@localhost"));
    }

    @Test
    void rejectsMalformedEmail() {
        assertThrows(ResponseStatusException.class, () -> validator.validateEmailDomainMx("not-an-email"));
        assertThrows(ResponseStatusException.class, () -> validator.validateEmailDomainMx("user@"));
        assertThrows(ResponseStatusException.class, () -> validator.validateEmailDomainMx(null));
    }

    @Test
    void rejectsNonExistentDomain() {
        assertThrows(ResponseStatusException.class,
                () -> validator.validateEmailDomainMx("fake@this-domain-surely-does-not-exist-xyz1234567.com"));
    }

    @Test
    void bypassesValidationWhenDisabled() {
        EmailValidatorService disabledValidator = new EmailValidatorService(false);
        assertDoesNotThrow(() -> disabledValidator.validateEmailDomainMx("fake@this-domain-surely-does-not-exist-xyz1234567.com"));
    }

    @Test
    void cachesValidationResult() {
        // First check
        boolean first = validator.hasValidMxOrARecord("example.com");
        // Second check hits cache
        boolean second = validator.hasValidMxOrARecord("example.com");
        assertTrue(first);
        assertTrue(second);
    }
}
