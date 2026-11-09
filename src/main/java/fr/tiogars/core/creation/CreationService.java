package fr.tiogars.core.creation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import fr.tiogars.core.creation.exceptions.CreationException;
import fr.tiogars.core.validation.ValidationService;

/**
 * Base service for creating entities, alone or in batch.
 *
 * <p>Each creation follows the same pipeline:
 * {@link #prepare(Object) prepare} → validation by the
 * {@link ValidationService} →
 * {@link #persist(Object) persist} → {@link #afterCreate(Object) afterCreate}.
 * Subclasses must implement {@link #persist(Object)} and may override the
 * other hooks.
 *
 * <p>Batch creation via {@link #createAll(Collection)} prepares and validates
 * every entity before persisting any of them: if a single entity is invalid,
 * nothing is persisted.
 *
 * @param <T> type of the created entities
 */
public abstract class CreationService<T> {

    /** Service that validates entities before persistence. */
    private final ValidationService<? super T> validationService;

    /**
     * Creates a service that accepts every entity without validation.
     */
    protected CreationService() {
        this(new ValidationService<>());
    }

    /**
     * Creates a service that validates entities with the given service.
     *
     * @param validator validation service, must not be {@code null}
     * @throws NullPointerException if {@code validator} is {@code null}
     */
    protected CreationService(final ValidationService<? super T> validator) {
        this.validationService = Objects.requireNonNull(validator,
                "validationService must not be null");
    }

    /**
     * Creates a single entity.
     *
     * @param entity entity to create, must not be {@code null}
     * @return the created entity as returned by {@link #persist(Object)}
     * @throws NullPointerException if {@code entity} is {@code null}
     * @throws CreationException if the entity is invalid
     * @throws IllegalStateException if {@link #prepare(Object)} or
     *         {@link #persist(Object)} returns {@code null}
     */
    public final T create(final T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        T prepared = requirePrepared(entity);
        rejectIfInvalid(validationService.validate(prepared));
        T created = requirePersisted(persist(prepared));
        afterCreate(created);
        return created;
    }

    /**
     * Creates a set of entities with all-or-nothing validation.
     *
     * <p>All entities are prepared, then validated with
     * {@link ValidationService#validateAll(List)}; violations of the whole
     * batch are reported in a single {@link CreationException}. Persistence
     * only starts when the whole batch is valid.
     *
     * @param entities entities to create, must not be {@code null} nor contain
     *        {@code null}
     * @return an unmodifiable list of created entities, in input order; empty
     *         if the input is empty
     * @throws NullPointerException if {@code entities} is {@code null} or
     *         contains {@code null}
     * @throws CreationException if at least one entity is invalid or duplicated
     * @throws IllegalStateException if a hook returns {@code null} or
     *         {@link #persistAll(List)} returns a list of a different size
     */
    public final List<T> createAll(final Collection<? extends T> entities) {
        Objects.requireNonNull(entities, "entities must not be null");
        List<T> prepared = new ArrayList<>(entities.size());
        for (T entity : entities) {
            Objects.requireNonNull(entity, "entities must not contain null");
            prepared.add(requirePrepared(entity));
        }
        rejectIfInvalid(validationService.validateAll(prepared));
        if (prepared.isEmpty()) {
            return List.of();
        }
        List<T> created = persistAll(List.copyOf(prepared));
        if (created == null || created.size() != prepared.size()) {
                throw new IllegalStateException(
                        "persistAll must return one created entity "
                            + "per input entity");
        }
        created.forEach(this::requirePersisted);
        created.forEach(this::afterCreate);
        return List.copyOf(created);
    }

    /**
     * Normalizes or completes an entity before validation (trimming, default
     * values...). Default implementation returns the entity unchanged.
     *
     * @param entity entity to prepare, never {@code null}
     * @return the prepared entity, which must not be {@code null}
     */
    protected T prepare(final T entity) {
        return entity;
    }

    /**
     * Persists a single valid entity.
     *
     * @param entity prepared and validated entity, never {@code null}
     * @return the created entity (for example with a generated identifier),
     *         which must not be {@code null}
     */
    protected abstract T persist(T entity);

    /**
     * Persists a valid batch. Default implementation calls
     * {@link #persist(Object)} for each entity; override it to use a bulk or
     * transactional operation.
     *
     * @param entities unmodifiable, non-empty list of prepared and validated
     *        entities
     * @return created entities in the same order and of the same size
     */
    protected List<T> persistAll(final List<T> entities) {
        return entities.stream().map(this::persist).toList();
    }

    /**
     * Called after each successful creation (events, audit, cache...).
     * For a batch, it is called once per entity after the whole batch is
     * persisted. Default implementation does nothing.
     *
     * @param created created entity, never {@code null}
     */
    protected void afterCreate(final T created) {
        // no-op by default
    }

    /**
     * Prepares an entity and enforces the non-null hook contract.
     *
     * @param entity entity to prepare
     * @return the prepared entity
     */
    private T requirePrepared(final T entity) {
        T prepared = prepare(entity);
        if (prepared == null) {
            throw new IllegalStateException("prepare must not return null");
        }
        return prepared;
    }

    /**
     * Throws when validation reports one or more violations.
     *
     * @param violations validation messages
     */
    private static void rejectIfInvalid(final List<String> violations) {
        if (!violations.isEmpty()) {
            throw new CreationException(violations);
        }
    }

    /**
     * Enforces the non-null persistence hook contract.
     *
     * @param created persisted entity
     * @return the persisted entity
     */
    private T requirePersisted(final T created) {
        if (created == null) {
            throw new IllegalStateException("persist must not return null");
        }
        return created;
    }
}
