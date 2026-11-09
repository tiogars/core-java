package fr.tiogars.core.creation.exceptions;

import java.util.List;
import java.util.Objects;

/**
 * Thrown by
 * {@link fr.tiogars.core.creation.services.CreationService} when one or more
 * entities cannot be created because they are invalid. No entity is persisted
 * when this exception is thrown.
 */
public class CreationException extends RuntimeException {

    /** Serialization version identifier. */
    private static final long serialVersionUID = 1L;

    /** Immutable violation messages associated with this exception. */
    private final List<String> violations;

    /**
     * Creates an exception carrying the given violations.
     *
     * @param violationMessages violation messages, must not be {@code null} nor
     *        contain {@code null}
     */
    public CreationException(final List<String> violationMessages) {
        super("Creation rejected: " + String.join("; ", Objects.requireNonNull(
                violationMessages, "violations must not be null")));
        this.violations = List.copyOf(violationMessages);
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
