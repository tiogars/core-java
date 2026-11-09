# core-java

Bibliothèque Java commune pour valider des entités et orchestrer leur création, individuellement ou par lots.

## Éléments disponibles

| Élément | Objectif |
| --- | --- |
| `ValidationService<T>` | Appliquer des règles métier et détecter les doublons dans un lot. |
| `CreationService<T>` | Enchaîner préparation, validation, persistance et traitement après création. |
| `CreationException` | Restituer les violations qui empêchent la création. |

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
mvn clean site
```

Consulter les [instructions de contribution](CONTRIBUTING.md) et les [règles de développement](AGENTS.md). La documentation doit être actualisée avec chaque évolution de la bibliothèque.
