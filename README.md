# core-java

Bibliothèque Java commune pour valider des entités et orchestrer leur création, individuellement ou par lots.

## Éléments disponibles

| Élément | Objectif |
| --- | --- |
| `fr.tiogars.core.validation.services.ValidationService<T>` | Appliquer des règles métier et détecter les doublons dans un lot. |
| `CreationService<T>` | Enchaîner préparation, validation, contrôle d'existant, persistance et traitement après création, avec `create`, `createIfAbsent` et `createAll`. |
| `CreationException` | Restituer les violations qui empêchent la création. |
| `DuplicateCreationException` | Signaler qu'une entité équivalente existe déjà dans le stockage. |

Le [guide d'utilisation](docs/index.md) décrit chaque élément, ses limites et un exemple Java complet de création d'utilisateurs en mémoire.

## Utilisation locale

Prérequis : JDK 25 et Maven installé (pas de wrapper Maven dans ce dépôt).

Depuis la racine du dépôt, installer la bibliothèque dans le dépôt Maven local :

```shell
mvn clean install
```

Ajouter ensuite cette dépendance au projet consommateur utilisant Java 25 :

```xml
<dependency>
	<groupId>fr.tiogars</groupId>
	<artifactId>core</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```

Cette procédure utilise le dépôt Maven local et ne nécessite pas de configuration GitHub Packages.

## Développement

```shell
mvn test
mvn clean package
mvn clean test site
```

Le site Maven inclut les pages navigables des sources Java et des tests (`Source Xref` et `Test Source Xref`), ainsi que le rapport de couverture JaCoCo dans `target/site/jacoco/index.html`. Les tests doivent précéder `site` pour collecter la couverture; `mvn clean site` seul ne la produit pas. `mvn test` produit également le rapport local. Consulter le [guide de couverture](docs/index.md#couverture-de-code) pour son utilisation à chaque itération.

Le workflow [Deploy Maven Site to GitHub Pages](.github/workflows/maven-site.yml) publie le site Maven lors d'un push sur `main` ou d'un lancement manuel. Il transmet `target/site` aux actions officielles GitHub Pages, puis le déploie dans l'environnement `github-pages`, sans utiliser de branche `gh-pages`. Dans les paramètres Pages du dépôt, la source doit être **GitHub Actions**. Si la génération du site échoue, le déploiement n'est pas lancé.

Consulter les [instructions de contribution](CONTRIBUTING.md) et les [règles de développement](AGENTS.md). La documentation doit être actualisée avec chaque évolution de la bibliothèque.
