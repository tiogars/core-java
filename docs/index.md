# core

Core library in Java.

## Creation and validation services

`fr.tiogars.core.services.CreationService<T>` is an abstract base class for creating one or several entities through a common pipeline: `prepare` → validation → `persist` → `afterCreate`. Validation is delegated to a `ValidationService<T>` passed to the constructor (by default, every entity is accepted).

- `create(T)` creates a single entity.
- `createAll(Collection<? extends T>)` creates a batch with all-or-nothing validation: every entity is prepared and validated before any is persisted.
- Invalid entities raise a `CreationException` whose `getViolations()` lists every problem.

`fr.tiogars.core.services.ValidationService<T>` can also be used on its own:

- `validate(T)` returns the violations of one entity, defined by overriding `check(T)`.
- `validateAll(List<? extends T>)` returns the violations of a batch, prefixed by `[index]`, and reports duplicates identified by `uniqueKey(T)`.

```java
class UserValidationService extends ValidationService<User> {
    @Override
    protected List<String> check(User user) {
        return user.email().isEmpty() ? List.of("email is required") : List.of();
    }

    @Override
    protected Object uniqueKey(User user) {
        return user.email();
    }
}

class UserCreationService extends CreationService<User> {
    UserCreationService() {
        super(new UserValidationService());
    }

    @Override
    protected User prepare(User user) {
        return user.withEmail(user.email().trim().toLowerCase());
    }

    @Override
    protected User persist(User user) {
        return repository.save(user);
    }
}
```

Override `persistAll(List<T>)` to use a bulk or transactional operation.
