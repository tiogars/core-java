package fr.tiogars.core.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Verifies individual validation, indexed batch errors and duplicate rules. */
class ValidationServiceTest {

    /** Rejects blank names and detects duplicate non-blank names. */
    static class NameValidationService extends ValidationService<String> {
        /**
         * Reports blank names and exercises normalization of null violations.
         *
         * @param entity non-null name to validate
         * @return a violation for a blank name, otherwise null
         */
        @Override
        protected List<String> check(final String entity) {
            return entity.isBlank() ? List.of("name is required") : null;
        }

        /**
         * Excludes blank names from duplicate detection.
         *
         * @param entity non-null name to identify
         * @return the name, or null to skip duplicate detection
         */
        @Override
        protected Object uniqueKey(final String entity) {
            return entity.isBlank() ? null : entity;
        }
    }

    /** Validator reused by the test cases. */
    private final NameValidationService service = new NameValidationService();

    /** Verifies blank-name errors and null normalization to an empty list. */
    @Test
    void validateReturnsViolationsOrEmptyList() {
        assertEquals(List.of("name is required"), service.validate(" "));
        assertTrue(service.validate("a").isEmpty());
    }

    /** Verifies that validating a null name raises a null pointer exception. */
    @Test
    void validateRejectsNull() {
        assertThrows(NullPointerException.class, () -> service.validate(null));
    }

    /** Verifies ordered, indexed errors referencing the first duplicate. */
    @Test
    void validateAllPrefixesViolationsWithIndexAndReportsDuplicates() {
        List<String> violations = service.validateAll(
            List.of("a", "", "a", "b", "b"));

        assertEquals(List.of(
                "[1] name is required",
                "[2] duplicate of entity at index 0",
                "[4] duplicate of entity at index 3"), violations);
    }

    /** Verifies that a null batch or null name is rejected. */
    @Test
    void validateAllRejectsNullListOrElement() {
        assertThrows(NullPointerException.class,
            () -> service.validateAll(null));
        List<String> entitiesWithNull = Arrays.asList("a", null);
        assertThrows(NullPointerException.class,
            () -> service.validateAll(entitiesWithNull));
    }

    /** Verifies that default validation accepts blank and duplicate names. */
    @Test
    void defaultServiceAcceptsEverythingIncludingDuplicates() {
        ValidationService<String> defaults = new ValidationService<>();

        assertTrue(defaults.validate("").isEmpty());
        assertTrue(defaults.validateAll(List.of("x", "x")).isEmpty());
    }
}
