package fr.tiogars.core.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validates entities, alone or in batch, and reports violations as messages.
 *
 * <p>Subclasses define the rules by overriding {@link #check(Object)} and,
 * to detect duplicates within a batch, {@link #uniqueKey(Object)}. The default
 * implementation accepts every entity.
 *
 * @param <T> type of the validated entities
 */
public class ValidationService<T> {

    /**
     * Validates a single entity.
     *
     * @param entity entity to validate, must not be {@code null}
     * @return an unmodifiable list of violations; empty when the entity is valid
     * @throws NullPointerException if {@code entity} is {@code null}
     */
    public final List<String> validate(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        List<String> violations = check(entity);
        return violations == null ? List.of() : List.copyOf(violations);
    }

    /**
     * Validates a batch. Each violation is prefixed with the zero-based index of
     * its entity (for example {@code "[2] name is required"}), and entities sharing
     * the same non-null {@link #uniqueKey(Object)} are reported as duplicates.
     *
     * @param entities entities to validate, must not be {@code null} nor contain {@code null}
     * @return an unmodifiable list of violations for the whole batch; empty when all entities are valid
     * @throws NullPointerException if {@code entities} is {@code null} or contains {@code null}
     */
    public final List<String> validateAll(List<? extends T> entities) {
        Objects.requireNonNull(entities, "entities must not be null");
        List<String> violations = new ArrayList<>();
        Map<Object, Integer> firstIndexByKey = new HashMap<>();
        for (int index = 0; index < entities.size(); index++) {
            T entity = entities.get(index);
            for (String violation : validate(entity)) {
                violations.add("[" + index + "] " + violation);
            }
            Object key = uniqueKey(entity);
            if (key != null) {
                Integer first = firstIndexByKey.putIfAbsent(key, index);
                if (first != null) {
                    violations.add("[" + index + "] duplicate of entity at index " + first);
                }
            }
        }
        return List.copyOf(violations);
    }

    /**
     * Applies the validation rules to an entity. Default implementation accepts every entity.
     *
     * @param entity entity to check, never {@code null}
     * @return violation messages; empty or {@code null} when the entity is valid
     */
    protected List<String> check(T entity) {
        return List.of();
    }

    /**
     * Returns the business key used to detect duplicates within a batch.
     * Default implementation returns {@code null}, which disables the check.
     *
     * @param entity entity, never {@code null}
     * @return the key, compared with {@code equals}/{@code hashCode}, or {@code null} to skip the check
     */
    protected Object uniqueKey(T entity) {
        return null;
    }
}
