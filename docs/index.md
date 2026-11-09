# core-java : guide d'utilisation

Cette bibliothèque fournit des briques réutilisables de validation et de création d'entités. Elle ne fournit ni base de données, ni dépôt métier, ni gestionnaire de transactions. Les exemples ciblent les développeurs Java qui intègrent ces services dans leur application, avec un JDK 25.

## Rôles et domaines

| Type à importer | Objectif | Quand l'utiliser |
| --- | --- | --- |
| `fr.tiogars.core.validation.services.ValidationService<T>` | Centraliser les règles métier et les contrôles de doublons. | Vérifier une entité ou un lot, même sans création. |
| `fr.tiogars.core.creation.services.CreationService<T>` | Orchestrer le cycle de création en déléguant la validation. | Partager un même enchaînement entre créations unitaires et par lots. |
| `fr.tiogars.core.creation.exceptions.CreationException` | Transporter les messages de rejet d'une création. | Présenter les violations au consommateur sans analyser un message d'exception. |
| `fr.tiogars.core.creation.exceptions.DuplicateCreationException` | Signaler qu'une entité équivalente existe déjà dans le stockage. | Réagir spécifiquement aux doublons, par exemple avec un statut HTTP 409. |

La création dépend du domaine autonome de validation, jamais l'inverse. Les règles restent dans le validateur; les hooks de préparation, de persistance et de notification permettent d'adapter l'orchestration sans ajouter de dépendance à une application cliente.

## ValidationService

### Contrats

| Élément | Objectif et comportement |
| --- | --- |
| `new ValidationService<>()` | Validateur neutre : accepte toute entité non nulle, sans détection de doublons. |
| `validate(T)` | Retourne les violations d'une entité dans une liste non modifiable, vide si elle est valide. |
| `validateAll(List<? extends T>)` | Agrège les violations et les doublons du lot. Les messages sont préfixés par l'indice à partir de zéro, par exemple `[1] email is required`. |
| `check(T)` | Hook à redéfinir pour appliquer les règles. Retourne une liste vide ou `null` si l'entité est valide. Les messages eux-mêmes ne doivent pas être nuls. |
| `uniqueKey(T)` | Hook à redéfinir pour identifier les doublons avec `equals` et `hashCode`. Une clé nulle désactive le contrôle pour cette entité. |

Une entrée nulle, une liste nulle ou un élément nul provoque une `NullPointerException`. Une liste vide est valide. Le validateur ne prépare pas les données : hors du service de création, le consommateur doit effectuer lui-même toute normalisation nécessaire. La détection des doublons porte uniquement sur le lot fourni, pas sur les données déjà persistées.

### Exemple autonome de validation

Ce programme est complet et ne dépend que de la bibliothèque et du JDK. Enregistrer cet exemple dans un fichier nommé `ValidationExample.java` au sein du projet consommateur.

```java
import java.util.List;

import fr.tiogars.core.validation.services.ValidationService;

public class ValidationExample {
    public static void main(String[] args) {
        ValidationService<String> validator = new ValidationService<>() {
            @Override
            protected List<String> check(String email) {
                return email.isBlank() ? List.of("email is required") : List.of();
            }

            @Override
            protected Object uniqueKey(String email) {
                return email;
            }
        };

        System.out.println(validator.validate("alice@example.org"));
        System.out.println(validator.validate(" "));
        System.out.println(validator.validateAll(List.of("alice@example.org", "alice@example.org")));
    }
}
```

Résultat attendu :

```text
[]
[email is required]
[[1] duplicate of entity at index 0]
```

## CreationService

### Contrats et points d'extension

Le pipeline unitaire est `prepare` → `validate` → `findExisting` → `persist` → `afterCreate`. Les méthodes publiques `create`, `createIfAbsent` et `createAll` sont finales : les adaptations passent par les hooks protégés.

| Élément | Objectif et comportement |
| --- | --- |
| Constructeur sans argument | Utilise un validateur neutre. Seule la persistance doit être implémentée. |
| Constructeur avec `ValidationService<? super T>` | Injecte les règles métier; refuse un validateur nul. |
| `create(T)` | Prépare, valide et persiste une entité, puis appelle `afterCreate`. Retourne le résultat de `persist`. Lève `DuplicateCreationException` si `findExisting` signale une entité équivalente déjà persistée. |
| `createIfAbsent(T)` | Même pipeline que `create`, mais une entité déjà existante retourne un `Optional` vide au lieu de lever une exception. Retourne sinon l'entité créée dans un `Optional`. |
| `createAll(Collection<? extends T>)` | Prépare toutes les entités, valide le lot, vérifie l'absence de chaque entité dans le stockage, puis persiste. Retourne une liste non modifiable dans l'ordre d'entrée. Un lot vide retourne une liste vide sans persistance ni notification. Les entités déjà persistées sont signalées ensemble par une `DuplicateCreationException` aux messages indexés, par exemple `[1] entity already exists`. |
| `prepare(T)` | Normalise ou complète l'entité avant validation. Retourne par défaut l'entité inchangée. |
| `findExisting(T)` | Hook de recherche d'une entité équivalente déjà persistée, par exemple par clé métier auprès d'un dépôt. Reçoit l'entité préparée. Retourne par défaut un `Optional` vide, ce qui désactive le contrôle. Ce contrôle est distinct de `uniqueKey`, qui ne détecte les doublons qu'au sein du lot fourni. |
| `persist(T)` | Hook abstrait obligatoire : enregistre l'entité et retourne sa version créée, par exemple avec un identifiant. |
| `persistAll(List<T>)` | Reçoit une liste non modifiable, non vide et validée. Par défaut, un stream séquentiel appelle `persist` dans l'ordre; redéfinir ce hook pour une opération groupée ou transactionnelle. |
| `afterCreate(T)` | Hook sans effet par défaut, destiné aux événements, à l'audit ou au cache. En lot, il est appelé pour chaque entité seulement après la persistance complète et le contrôle de ses résultats. |

Une entrée nulle ou un élément nul provoque une `NullPointerException`. Un résultat nul de `prepare`, `persist` ou `findExisting`, une liste nulle de `persistAll`, un élément nul de cette liste ou une taille différente de celle du lot provoque une `IllegalStateException`. `persistAll` doit préserver l'ordre des entités; seule la taille et l'absence de résultats nuls sont contrôlées.

### Exemple complet : création en mémoire

Ce programme définit ses propres entités et son stockage, sans dépôt externe ni framework. L'adresse est normalisée avant la validation, sert de clé de détection des doublons dans un lot et de clé de recherche d'un utilisateur déjà stocké via `findExisting`. La règle illustrative impose seulement une adresse non vide; ce n'est pas une validation complète du format d'une adresse électronique.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import fr.tiogars.core.creation.exceptions.CreationException;
import fr.tiogars.core.creation.exceptions.DuplicateCreationException;
import fr.tiogars.core.creation.services.CreationService;
import fr.tiogars.core.validation.services.ValidationService;

public class CreationExample {
    record User(Long id, String email) {}

    static class UserValidationService extends ValidationService<User> {
        @Override
        protected List<String> check(User user) {
            return user.email() == null || user.email().isBlank()
                    ? List.of("email is required") : List.of();
        }

        @Override
        protected Object uniqueKey(User user) {
            return user.email();
        }
    }

    static class UserCreationService extends CreationService<User> {
        private final List<User> stored = new ArrayList<>();
        private long sequence;

        UserCreationService() {
            super(new UserValidationService());
        }

        @Override
        protected User prepare(User user) {
            String email = user.email();
            return new User(user.id(), email == null ? null : email.trim().toLowerCase(Locale.ROOT));
        }

        @Override
        protected User persist(User user) {
            User created = new User(++sequence, user.email());
            stored.add(created);
            return created;
        }

        @Override
        protected Optional<User> findExisting(User user) {
            return stored.stream()
                    .filter(candidate -> candidate.email().equals(user.email()))
                    .findFirst();
        }

        @Override
        protected void afterCreate(User created) {
            System.out.println("Created: " + created.email());
        }
    }

    public static void main(String[] args) {
        UserCreationService service = new UserCreationService();
        User alice = service.create(new User(null, "  ALICE@example.org "));
        System.out.println(alice);

        List<User> batch = service.createAll(List.of(
                new User(null, "bob@example.org"),
                new User(null, "carol@example.org")));
        System.out.println(batch.size());

        try {
            service.createAll(List.of(
                    new User(null, "dave@example.org"),
                    new User(null, " DAVE@example.org "),
                    new User(null, " ")));
        } catch (CreationException exception) {
            exception.getViolations().forEach(System.out::println);
        }

        System.out.println(service.createIfAbsent(
                new User(null, "Alice@example.org")).isEmpty());

        try {
            service.create(new User(null, "bob@example.org"));
        } catch (DuplicateCreationException exception) {
            exception.getViolations().forEach(System.out::println);
        }
        System.out.println(service.stored.size());
    }
}
```

Dans le projet consommateur, nommer le fichier `CreationExample.java`. Résultat attendu :

```text
Created: alice@example.org
User[id=1, email=alice@example.org]
Created: bob@example.org
Created: carol@example.org
2
[1] duplicate of entity at index 0
[2] email is required
true
entity already exists
3
```

Le lot rejeté n'ajoute personne au stockage, l'utilisatrice déjà stockée n'est pas recréée par `createIfAbsent` et le doublon unitaire est rejeté par une `DuplicateCreationException` : il reste trois utilisateurs. Cet exemple de stockage en mémoire n'est ni durable ni conçu pour des accès concurrents.

## CreationException

**Objectif :** signaler les violations métier qui empêchent une création. Cette exception non vérifiée est levée par `create` et `createAll` lorsque le validateur retourne des violations. L'exemple précédent montre comment l'intercepter et parcourir `getViolations()` pour afficher chaque problème.

Le constructeur `CreationException(List<String>)` permet aussi de construire ce rejet explicitement avec des messages métier. Il refuse une liste nulle ou contenant un message nul et conserve une copie non modifiable des violations. `getViolations()` retourne cette copie. `getMessage()` produit un résumé préfixé par `Creation rejected: `; pour traiter les erreurs, utiliser les violations structurées plutôt qu'analyser ce résumé.

## DuplicateCreationException

**Objectif :** signaler qu'une création est rejetée parce qu'une entité équivalente existe déjà dans le stockage, d'après le hook `findExisting`. Cette exception non vérifiée est une sous-classe de `CreationException` : un consommateur qui rejette uniformément toutes les créations invalides peut intercepter le type parent, tandis qu'un consommateur qui distingue le conflit, par exemple pour répondre avec un statut HTTP 409, intercepte ce type. L'exemple précédent montre son interception lors d'une création unitaire; `createAll` la lève aussi avec un message indexé par entité, par exemple `[1] entity already exists`. Aucune entité n'est persistée quand elle est levée.

Le constructeur `DuplicateCreationException(List<String>)` applique les mêmes contrats que `CreationException` : liste non nulle, sans message nul, copiée de manière non modifiable.

## Limites des lots et des transactions

- Si la validation du lot trouve une violation ou un doublon, ou si une entité du lot existe déjà dans le stockage, aucune entité du lot n'est persistée par l'orchestrateur.
- Cela ne garantit pas une transaction : si le troisième appel à `persist` échoue, les deux premiers enregistrements peuvent déjà exister. Aucune annulation automatique n'est prévue.
- Pour un enregistrement atomique, redéfinir `persistAll` avec le mécanisme transactionnel du stockage choisi. Retourner une entité créée par entrée, dans le même ordre.
- Le contrôle `findExisting` puis `persist` n'est pas atomique : deux créations concurrentes de la même entité peuvent toutes deux passer le contrôle. La garantie d'unicité définitive reste à la charge du stockage, par exemple avec une contrainte d'unicité en base; le service fournit un contrôle métier préalable et des messages exploitables.
- La gestion des transactions appartient au projet consommateur : par exemple, une méthode `@Transactional` d'un service Spring peut déléguer à `create`, `createIfAbsent` ou `createAll`.
- Si `afterCreate` échoue, les entités sont déjà persistées. Les hooks suivants peuvent ne pas être appelés; l'exception est propagée, sans rollback automatique.
- Les exceptions des hooks et du validateur sont propagées, pas converties automatiquement en `CreationException`. Éviter les effets de bord dans `prepare`, dans `findExisting` et dans les règles de validation.

## Compatibilité des imports

Les exemples utilisent `fr.tiogars.core.creation.services.CreationService`, `fr.tiogars.core.creation.exceptions.CreationException` et `fr.tiogars.core.validation.services.ValidationService`. Le déplacement de `ValidationService` depuis `fr.tiogars.core.validation` vers `fr.tiogars.core.validation.services` modifie son nom pleinement qualifié et constitue une rupture de compatibilité source et binaire. Les projets consommateurs doivent remplacer l'ancien import `fr.tiogars.core.validation.ValidationService` par `fr.tiogars.core.validation.services.ValidationService`, puis recompiler.

## Couverture de code

JaCoCo mesure la couverture des classes de production par les tests unitaires, sans modifier l'API ni ajouter de dépendance à la bibliothèque distribuée. L'agent est préparé avant les tests; le rapport est généré après leur exécution pendant la phase `test`.

Pour obtenir une mesure propre et le site Maven complet :

```shell
mvn clean test site
```

Ouvrir `target/site/index.html`, puis le rapport **JaCoCo** dans les rapports du projet, ou ouvrir directement `target/site/jacoco/index.html`. Le rapport permet de naviguer par package, classe et source pour inspecter les instructions, lignes, branches et méthodes couvertes ou manquées. Les fichiers `target/site/jacoco/jacoco.xml` et `target/site/jacoco/jacoco.csv` contiennent les mesures exploitables par d'autres outils.

Pour une itération locale, `mvn test` actualise également le rapport. Utiliser `mvn clean test` pour éviter de conserver des classes ou des données de couverture d'une exécution précédente. Avant les modifications, relever les compteurs des classes concernées et conserver cette référence hors de `target`, car `clean` supprime ce répertoire. Après les modifications, comparer les lignes et branches couvertes ou manquées, compléter les tests des nouveaux chemins et expliquer toute régression justifiée dans le bilan. Aucun seuil global bloquant n'est configuré; le pourcentage global ne remplace pas l'examen du code modifié.

`mvn clean site` seul n'exécute pas les tests et ne produit donc pas de rapport de couverture après nettoyage. Si JaCoCo signale l'absence de données d'exécution ou si le rapport manque, relancer `mvn clean test site` sans désactiver les tests ni l'agent. Un échec des tests doit être corrigé avant de considérer le rapport comme validé. Le workflow GitHub Pages exécute aussi les tests avant de publier le site et sa couverture.
