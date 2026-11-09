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
        protected List<String> check(Item entity) {
            return entity.name() == null || entity.name().isEmpty() ? List.of("name is required") : List.of();
        }

        @Override
        protected Object uniqueKey(Item entity) {
            return entity.name();
        }
    }

    static class ItemCreationService extends CreationService<Item> {
        final List<Item> store = new ArrayList<>();
        final List<Item> notified = new ArrayList<>();
        long sequence = 0;

        ItemCreationService() {
            super(new ItemValidationService());
        }

        @Override
        protected Item prepare(Item entity) {
            return new Item(entity.id(), entity.name() == null ? null : entity.name().trim());
        }

        @Override
        protected Item persist(Item entity) {
            Item created = new Item(++sequence, entity.name());
            store.add(created);
            return created;
        }

        @Override
        protected void afterCreate(Item created) {
            notified.add(created);
        }
    }

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
        CreationException ex = assertThrows(CreationException.class, () -> service.create(new Item(null, "   ")));

        assertEquals(List.of("name is required"), ex.getViolations());
        assertTrue(service.store.isEmpty());
        assertTrue(service.notified.isEmpty());
    }

    @Test
    void createAllCreatesEveryEntityInOrder() {
        List<Item> created = service.createAll(List.of(new Item(null, "a"), new Item(null, "b")));

        assertEquals(List.of(new Item(1L, "a"), new Item(2L, "b")), created);
        assertEquals(created, service.notified);
        assertThrows(UnsupportedOperationException.class, () -> created.add(new Item(null, "c")));
    }

    @Test
    void createAllWithEmptyCollectionReturnsEmptyList() {
        assertTrue(service.createAll(List.of()).isEmpty());
        assertTrue(service.store.isEmpty());
    }

    @Test
    void createAllIsAllOrNothingAndReportsAllViolations() {
        List<Item> input = List.of(new Item(null, "a"), new Item(null, ""), new Item(null, " a "), new Item(null, null));

        CreationException ex = assertThrows(CreationException.class, () -> service.createAll(input));

        assertEquals(List.of(
                "[1] name is required",
                "[2] duplicate of entity at index 0",
                "[3] name is required"), ex.getViolations());
        assertTrue(service.store.isEmpty());
        assertTrue(service.notified.isEmpty());
    }

    @Test
    void createAllRejectsNullCollectionOrElement() {
        assertThrows(NullPointerException.class, () -> service.createAll(null));
        assertThrows(NullPointerException.class, () -> service.createAll(Arrays.asList(new Item(null, "a"), null)));
        assertTrue(service.store.isEmpty());
    }

    @Test
    void persistReturningNullIsRejected() {
        CreationService<Item> broken = new CreationService<>() {
            @Override
            protected Item persist(Item entity) {
                return null;
            }
        };

        assertThrows(IllegalStateException.class, () -> broken.create(new Item(null, "a")));
        assertThrows(IllegalStateException.class, () -> broken.createAll(List.of(new Item(null, "a"))));
    }

    @Test
    void persistAllReturningWrongSizeIsRejected() {
        CreationService<Item> broken = new CreationService<>() {
            @Override
            protected Item persist(Item entity) {
                return entity;
            }

            @Override
            protected List<Item> persistAll(List<Item> entities) {
                return List.of();
            }
        };

        assertThrows(IllegalStateException.class, () -> broken.createAll(List.of(new Item(null, "a"))));
    }

    @Test
    void constructorRejectsNullValidationService() {
        assertThrows(NullPointerException.class, () -> new CreationService<String>(null) {
            @Override
            protected String persist(String entity) {
                return entity;
            }
        });
    }

    @Test
    void defaultHooksAcceptEntityAsIs() {
        CreationService<String> service = new CreationService<>() {
            @Override
            protected String persist(String entity) {
                return entity;
            }
        };

        assertEquals(List.of("x", "x"), service.createAll(List.of("x", "x")));
    }
}
