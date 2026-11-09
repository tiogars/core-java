package fr.tiogars.core.creation.exceptions;

import java.util.List;

/**
 * Thrown by
 * {@link fr.tiogars.core.creation.services.CreationService} when one or more
 * entities cannot be created because an equivalent entity already exists in
 * the underlying storage. No entity is persisted when this exception is
 * thrown.
 *
 * <p>This exception is a {@link CreationException}: consumers that only need
 * to reject a creation can catch the parent type, while consumers that need
 * to react specifically to duplicates (for example with an HTTP 409 status)
 * can catch this type.
 */
public class DuplicateCreationException extends CreationException {

    /** Serialization version identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception carrying the given duplicate violations.
     *
     * @param violationMessages violation messages, must not be {@code null}
     *        nor contain {@code null}
     */
    public DuplicateCreationException(final List<String> violationMessages) {
        super(violationMessages);
    }
}
