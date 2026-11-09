package fr.tiogars.core.creation.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.tiogars.core.creation.exceptions.CreationException;
import fr.tiogars.core.creation.exceptions.DuplicateCreationException;
import fr.tiogars.core.validation.services.ValidationService;

/**
 * Verifies creation orchestration, validation, duplicate detection and
 * persistence contracts.
 */
class CreationServiceTest {

    /**
     * Entity used to observe preparation and generated identifiers.
     *
     * @param id persisted identifier, or null before creation
     * @param name entity name used for validation and duplicate detection
     */
    record Item(Long id, String name) {
    }

    /** Requires a name and detects items sharing the same name. */
    static class ItemValidationService extends ValidationService<Item> {
        /**
         * Reports a missing name after preparation.
         *
         * @param entity prepared item to validate
         * @return the name violation, or an empty list for a valid name
         */
        @Override
        protected List<String> check(final Item entity) {
            return entity.name() == null || entity.name().isEmpty()
                    ? List.of("name is required") : List.of();
        }

        /**
         * Uses the prepared name as the duplicate detection key.
         *
         * @param entity prepared item to identify
         * @return the item name
         */
        @Override
        protected Object uniqueKey(final Item entity) {
            return entity.name();
        }
    }

    /** Records persistence and notifications for the creation scenarios. */
    static class ItemCreationService extends CreationService<Item> {
        /** Entities persisted by this test service. */
        private final List<Item> store = new ArrayList<>();
        /** Entities reported by the creation hook. */
        private final List<Item> notified = new ArrayList<>();
        /** Next generated test identifier. */
        private long sequence;

        /** Creates an in-memory service with name validation enabled. */
        ItemCreationService() {
            super(new ItemValidationService());
        }

        /**
         * Trims the name while preserving null for subsequent validation.
         *
         * @param entity item supplied by the test
         * @return a copy with the prepared name and unchanged identifier
         */
        @Override
        protected Item prepare(final Item entity) {
            return new Item(entity.id(),
                    entity.name() == null ? null : entity.name().trim());
        }

        /**
         * Assigns an identifier and records the persisted item.
         *
         * @param entity prepared and validated item
         * @return the stored item with its generated identifier
         */
        @Override
        protected Item persist(final Item entity) {
            Item created = new Item(++sequence, entity.name());
            store.add(created);
            return created;
        }

        /**
         * Looks up a stored item sharing the prepared name, emulating a
         * repository lookup by business key.
         *
         * @param entity prepared item to look up
         * @return the first stored item with the same name, if any
         */
        @Override
        protected Optional<Item> findExisting(final Item entity) {
            return store.stream()
                    .filter(item -> item.name().equals(entity.name()))
                    .findFirst();
        }

        /**
         * Records the notification emitted after successful persistence.
         *
         * @param created persisted item reported by the creation hook
         */
        @Override
        protected void afterCreate(final Item created) {
            notified.add(created);
        }
    }

    /** Fresh service instance initialized before each test. */
    private ItemCreationService service;

    /** Resets identifiers, stored items and notifications before each test. */
    @BeforeEach
    void setUp() {
        service = new ItemCreationService();
    }

    /** Verifies trimming, identifier assignment, storage and notification. */
    @Test
    void createPreparesPersistsAndNotifies() {
        Item created = service.create(new Item(null, "  alpha "));

        assertEquals(new Item(1L, "alpha"), created);
        assertEquals(List.of(created), service.store);
        assertEquals(List.of(created), service.notified);
    }

    /** Verifies that creating a null item raises a null pointer exception. */
    @Test
    void createRejectsNull() {
        assertThrows(NullPointerException.class, () -> service.create(null));
    }

    /**
     * Verifies that creating an item whose prepared name is already stored
     * raises a duplicate exception without persisting or notifying again.
     */
    @Test
    void createRejectsEntityAlreadyExisting() {
        Item first = service.create(new Item(null, "alpha"));
        Item duplicate = new Item(null, " alpha ");

        DuplicateCreationException ex = assertThrows(
                DuplicateCreationException.class,
                () -> service.create(duplicate));

        assertEquals(List.of("entity already exists"), ex.getViolations());
        assertEquals(List.of(first), service.store);
        assertEquals(List.of(first), service.notified);
    }

    /**
     * Verifies that an absent item is created, stored and notified, and that
     * the optional carries the persisted result.
     */
    @Test
    void createIfAbsentCreatesWhenNoEquivalentExists() {
        Optional<Item> created = service.createIfAbsent(
                new Item(null, "  alpha "));

        assertEquals(Optional.of(new Item(1L, "alpha")), created);
        assertEquals(List.of(new Item(1L, "alpha")), service.store);
        assertEquals(List.of(new Item(1L, "alpha")), service.notified);
    }

    /**
     * Verifies that an already stored equivalent item skips persistence and
     * notification and yields an empty optional instead of an exception.
     */
    @Test
    void createIfAbsentReturnsEmptyWhenEntityAlreadyExists() {
        Item first = service.create(new Item(null, "alpha"));

        Optional<Item> result = service.createIfAbsent(
                new Item(null, " alpha "));

        assertTrue(result.isEmpty());
        assertEquals(List.of(first), service.store);
        assertEquals(List.of(first), service.notified);
    }

    /**
     * Verifies that a null or invalid item is rejected by createIfAbsent
     * before any duplicate lookup or persistence.
     */
    @Test
    void createIfAbsentRejectsNullOrInvalidEntity() {
        Item blankItem = new Item(null, "   ");

        assertThrows(NullPointerException.class,
                () -> service.createIfAbsent(null));
        CreationException ex = assertThrows(CreationException.class,
                () -> service.createIfAbsent(blankItem));

        assertEquals(List.of("name is required"), ex.getViolations());
        assertTrue(service.store.isEmpty());
    }

    /** Verifies blank names fail without persistence or notification. */
    @Test
    void createRejectsInvalidEntityWithoutPersisting() {
        Item invalidItem = new Item(null, "   ");
        CreationException ex = assertThrows(CreationException.class,
                () -> service.create(invalidItem));

        assertEquals(List.of("name is required"), ex.getViolations());
        assertTrue(service.store.isEmpty());
        assertTrue(service.notified.isEmpty());
    }

    /** Verifies ordered creation and notifications with an immutable result. */
    @Test
    void createAllCreatesEveryEntityInOrder() {
        List<Item> created = service.createAll(
                List.of(new Item(null, "a"), new Item(null, "b")));
        Item additionalItem = new Item(null, "c");

        assertEquals(List.of(new Item(1L, "a"), new Item(2L, "b")), created);
        assertEquals(created, service.notified);
        assertThrows(UnsupportedOperationException.class,
                () -> created.add(additionalItem));
    }

    /** Verifies that an empty batch returns no items and stores nothing. */
    @Test
    void createAllWithEmptyCollectionReturnsEmptyList() {
        assertTrue(service.createAll(List.of()).isEmpty());
        assertTrue(service.store.isEmpty());
    }

    /**
     * Verifies indexed validation and duplicate errors prevent all persistence
     * and notifications for an invalid batch.
     */
    @Test
    void createAllIsAllOrNothingAndReportsAllViolations() {
        List<Item> input = List.of(new Item(null, "a"), new Item(null, ""),
                new Item(null, " a "), new Item(null, null));

        CreationException ex = assertThrows(CreationException.class,
                () -> service.createAll(input));

        assertEquals(List.of(
                "[1] name is required",
                "[2] duplicate of entity at index 0",
                "[3] name is required"), ex.getViolations());
        assertTrue(service.store.isEmpty());
        assertTrue(service.notified.isEmpty());
    }

    /** Verifies that a null batch or item is rejected without persistence. */
    @Test
    void createAllRejectsNullCollectionOrElement() {
        List<Item> itemsWithNull = Arrays.asList(new Item(null, "a"), null);
        assertThrows(NullPointerException.class,
                () -> service.createAll(null));
        assertThrows(NullPointerException.class,
                () -> service.createAll(itemsWithNull));
        assertTrue(service.store.isEmpty());
    }

    /**
     * Verifies that a batch containing items already stored is rejected with
     * indexed duplicate messages and without persisting the valid items.
     */
    @Test
    void createAllRejectsEntitiesAlreadyExisting() {
        Item first = service.create(new Item(null, "a"));
        List<Item> batch = List.of(new Item(null, "b"), new Item(null, " a "));

        DuplicateCreationException ex = assertThrows(
                DuplicateCreationException.class,
                () -> service.createAll(batch));

        assertEquals(List.of("[1] entity already exists"),
                ex.getViolations());
        assertEquals(List.of(first), service.store);
        assertEquals(List.of(first), service.notified);
    }

    /**
     * Verifies that a null existing lookup result fails for the single,
     * optional and batch entry points before any persistence.
     */
    @Test
    void findExistingReturningNullIsRejected() {
        CreationService<Item> broken = new CreationService<>() {
            /**
             * Supplies valid persistence for the lookup contract test double.
             *
             * @param entity item to return unchanged
             * @return the supplied item
             */
            @Override
            protected Item persist(final Item entity) {
                return entity;
            }

            /**
             * Simulates a lookup that violates the non-null result contract.
             *
             * @param entity item whose lookup is simulated
             * @return null to trigger the lookup result guard
             */
            @Override
            protected Optional<Item> findExisting(final Item entity) {
                return null;
            }
        };

        Item item = new Item(null, "a");
        List<Item> items = List.of(item);
        assertThrows(IllegalStateException.class, () -> broken.create(item));
        assertThrows(IllegalStateException.class,
                () -> broken.createIfAbsent(item));
        assertThrows(IllegalStateException.class,
                () -> broken.createAll(items));
    }

    /** Verifies null persistence results fail for single and batch calls. */
    @Test
    void persistReturningNullIsRejected() {
        CreationService<Item> broken = new CreationService<>() {
            /**
             * Simulates persistence that violates the non-null result contract.
             *
             * @param entity item whose persistence is simulated
             * @return null to trigger the persistence result guard
             */
            @Override
            protected Item persist(final Item entity) {
                return null;
            }
        };

        Item item = new Item(null, "a");
        List<Item> items = List.of(item);
        assertThrows(IllegalStateException.class, () -> broken.create(item));
        assertThrows(IllegalStateException.class,
                () -> broken.createAll(items));
    }

    /** Verifies that a preparation hook cannot return null. */
    @Test
    void prepareReturningNullIsRejected() {
        CreationService<Item> broken = new CreationService<>() {
            /**
             * Simulates preparation that violates the non-null hook contract.
             *
             * @param entity item whose preparation is simulated
             * @return null to trigger the preparation result guard
             */
            @Override
            protected Item prepare(final Item entity) {
                return null;
            }

            /**
             * Supplies the persistence hook required for the test service.
             *
             * @param entity item to return unchanged
             * @return the supplied item
             */
            @Override
            protected Item persist(final Item entity) {
                return entity;
            }
        };

        Item item = new Item(null, "a");
        assertThrows(IllegalStateException.class, () -> broken.create(item));
    }

    /** Verifies that batch persistence cannot return a null result list. */
    @Test
    void persistAllReturningNullIsRejected() {
        CreationService<Item> broken = new CreationService<>() {
            /**
             * Supplies valid single-item persistence for the batch test double.
             *
             * @param entity item to return unchanged
             * @return the supplied item
             */
            @Override
            protected Item persist(final Item entity) {
                return entity;
            }

            /**
             * Simulates batch persistence that violates its result contract.
             *
             * @param entities non-empty batch whose persistence is simulated
             * @return null to trigger the batch result guard
             */
            @Override
            protected List<Item> persistAll(final List<Item> entities) {
                return null;
            }
        };

        List<Item> items = List.of(new Item(null, "a"));
        assertThrows(IllegalStateException.class,
                () -> broken.createAll(items));
    }

    /** Verifies that a batch persistence result must match the input size. */
    @Test
    void persistAllReturningWrongSizeIsRejected() {
        CreationService<Item> broken = new CreationService<>() {
            /**
             * Supplies valid single-item persistence for the batch test double.
             *
             * @param entity item to return unchanged
             * @return the supplied item
             */
            @Override
            protected Item persist(final Item entity) {
                return entity;
            }

            /**
             * Simulates a batch persistence result with missing items.
             *
             * @param entities non-empty batch whose persistence is simulated
             * @return an empty list to trigger the result size guard
             */
            @Override
            protected List<Item> persistAll(final List<Item> entities) {
                return List.of();
            }
        };

        List<Item> items = List.of(new Item(null, "a"));
        assertThrows(IllegalStateException.class,
                () -> broken.createAll(items));
    }

    /** Verifies that construction rejects a null validation collaborator. */
    @Test
    void constructorRejectsNullValidationService() {
        assertThrows(NullPointerException.class,
                () -> new CreationService<String>(null) {
                    /**
                     * Supplies the persistence hook required for construction.
                     *
                     * @param entity value to return unchanged
                     * @return the supplied value
                     */
                    @Override
                    protected String persist(final String entity) {
                        return entity;
                    }
                });
    }

    /** Verifies that default hooks preserve values and accept duplicates. */
    @Test
    void defaultHooksAcceptEntityAsIs() {
        CreationService<String> defaultService = new CreationService<>() {
            /**
             * Leaves values unchanged to isolate the default hook behavior.
             *
             * @param entity value to persist
             * @return the supplied value
             */
            @Override
            protected String persist(final String entity) {
                return entity;
            }
        };

        assertEquals(List.of("x", "x"),
                defaultService.createAll(List.of("x", "x")));
    }
}
