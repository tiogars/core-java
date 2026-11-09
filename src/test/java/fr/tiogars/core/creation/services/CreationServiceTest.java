package fr.tiogars.core.creation.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.tiogars.core.creation.exceptions.CreationException;
import fr.tiogars.core.validation.ValidationService;

class CreationServiceTest {

    record Item(Long id, String name) {
    }

    static class ItemValidationService extends ValidationService<Item> {
        @Override
        protected List<String> check(final Item entity) {
            return entity.name() == null || entity.name().isEmpty()
                    ? List.of("name is required") : List.of();
        }

        @Override
        protected Object uniqueKey(final Item entity) {
            return entity.name();
        }
    }

    static class ItemCreationService extends CreationService<Item> {
        /** Entities persisted by this test service. */
        private final List<Item> store = new ArrayList<>();
        /** Entities reported by the creation hook. */
        private final List<Item> notified = new ArrayList<>();
        /** Next generated test identifier. */
        private long sequence;

        ItemCreationService() {
            super(new ItemValidationService());
        }

        @Override
        protected Item prepare(final Item entity) {
            return new Item(entity.id(),
                    entity.name() == null ? null : entity.name().trim());
        }

        @Override
        protected Item persist(final Item entity) {
            Item created = new Item(++sequence, entity.name());
            store.add(created);
            return created;
        }

        @Override
        protected void afterCreate(final Item created) {
            notified.add(created);
        }
    }

    /** Service instance reused by each test. */
    private ItemCreationService service;

    @BeforeEach
    void setUp() {
        service = new ItemCreationService();
    }

    @Test
    void createPreparesPersistsAndNotifies() {
        Item created = service.create(new Item(null, "  alpha "));

        assertEquals(new Item(1L, "alpha"), created);
        assertEquals(List.of(created), service.store);
        assertEquals(List.of(created), service.notified);
    }

    @Test
    void createRejectsNull() {
        assertThrows(NullPointerException.class, () -> service.create(null));
    }

    @Test
    void createRejectsInvalidEntityWithoutPersisting() {
        Item invalidItem = new Item(null, "   ");
        CreationException ex = assertThrows(CreationException.class,
                () -> service.create(invalidItem));

        assertEquals(List.of("name is required"), ex.getViolations());
        assertTrue(service.store.isEmpty());
        assertTrue(service.notified.isEmpty());
    }

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

    @Test
    void createAllWithEmptyCollectionReturnsEmptyList() {
        assertTrue(service.createAll(List.of()).isEmpty());
        assertTrue(service.store.isEmpty());
    }

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

    @Test
    void createAllRejectsNullCollectionOrElement() {
        List<Item> itemsWithNull = Arrays.asList(new Item(null, "a"), null);
        assertThrows(NullPointerException.class,
                () -> service.createAll(null));
        assertThrows(NullPointerException.class,
                () -> service.createAll(itemsWithNull));
        assertTrue(service.store.isEmpty());
    }

    @Test
    void persistReturningNullIsRejected() {
        CreationService<Item> broken = new CreationService<>() {
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

    @Test
    void persistAllReturningWrongSizeIsRejected() {
        CreationService<Item> broken = new CreationService<>() {
            @Override
            protected Item persist(final Item entity) {
                return entity;
            }

            @Override
            protected List<Item> persistAll(final List<Item> entities) {
                return List.of();
            }
        };

        List<Item> items = List.of(new Item(null, "a"));
        assertThrows(IllegalStateException.class,
                () -> broken.createAll(items));
    }

    @Test
    void constructorRejectsNullValidationService() {
        assertThrows(NullPointerException.class,
                () -> new CreationService<String>(null) {
                    @Override
                    protected String persist(final String entity) {
                        return entity;
                    }
                });
    }

    @Test
    void defaultHooksAcceptEntityAsIs() {
        CreationService<String> defaultService = new CreationService<>() {
            @Override
            protected String persist(final String entity) {
                return entity;
            }
        };

        assertEquals(List.of("x", "x"),
                defaultService.createAll(List.of("x", "x")));
    }
}
