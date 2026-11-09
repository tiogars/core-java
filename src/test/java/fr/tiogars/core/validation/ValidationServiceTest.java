package fr.tiogars.core.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class ValidationServiceTest {

    static class NameValidationService extends ValidationService<String> {
        @Override
        protected List<String> check(String entity) {
            return entity.isBlank() ? List.of("name is required") : null;
        }

        @Override
        protected Object uniqueKey(String entity) {
            return entity.isBlank() ? null : entity;
        }
    }

    private final NameValidationService service = new NameValidationService();

    @Test
    void validateReturnsViolationsOrEmptyList() {
        assertEquals(List.of("name is required"), service.validate(" "));
        assertTrue(service.validate("a").isEmpty());
    }

    @Test
    void validateRejectsNull() {
        assertThrows(NullPointerException.class, () -> service.validate(null));
    }

    @Test
    void validateAllPrefixesViolationsWithIndexAndReportsDuplicates() {
        List<String> violations = service.validateAll(List.of("a", "", "a", "b", "b"));

        assertEquals(List.of(
                "[1] name is required",
                "[2] duplicate of entity at index 0",
                "[4] duplicate of entity at index 3"), violations);
    }

    @Test
    void validateAllRejectsNullListOrElement() {
        assertThrows(NullPointerException.class, () -> service.validateAll(null));
        List<String> entitiesWithNull = Arrays.asList("a", null);
        assertThrows(NullPointerException.class, () -> service.validateAll(entitiesWithNull));
    }

    @Test
    void defaultServiceAcceptsEverythingIncludingDuplicates() {
        ValidationService<String> defaults = new ValidationService<>();

        assertTrue(defaults.validate("").isEmpty());
        assertTrue(defaults.validateAll(List.of("x", "x")).isEmpty());
    }
}
