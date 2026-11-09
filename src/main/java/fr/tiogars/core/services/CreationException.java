package fr.tiogars.core.services;

import java.util.List;
import java.util.Objects;

/**
 * Thrown by {@link CreationService} when one or more entities cannot be created
 * because they are invalid. No entity is persisted when this exception is thrown.
 */
public class CreationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> violations;

    /**
     * Creates an exception carrying the given violations.
     *
     * @param violations violation messages, must not be {@code null} nor contain {@code null}
     */
    public CreationException(List<String> violations) {
        super("Creation rejected: " + String.join("; ", Objects.requireNonNull(violations, "violations must not be null")));
        this.violations = List.copyOf(violations);
    }

    /**
     * Returns the violation messages.
     *
     * @return an unmodifiable list of violations
     */
    public List<String> getViolations() {
        return violations;
    }
}
