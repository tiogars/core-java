# Instructions pour les agents

## Objectif du projet

Ce dépôt contient une bibliothèque Java commune destinée à être réutilisée dans plusieurs projets. Privilégier une API publique claire, minimale et stable. Éviter les dépendances propres à une application cliente et préserver la compatibilité des API existantes, sauf demande explicite.

## Environnement et commandes

- Java cible : 25, configuré dans `pom.xml`.
- Build : Maven, sans wrapper présent dans le dépôt.
- Exécuter les tests unitaires : `mvn test`.
- Vérifier le build complet avant de terminer une implémentation : `mvn clean package`.
- Générer le site de documentation si la demande le concerne : `mvn clean site`.

Ne pas déclarer une vérification réussie si la commande n'a pas été exécutée. Si une commande ne peut pas être lancée, le signaler clairement.

## Implémentation Java

- Placer le code sous `src/main/java/fr/tiogars/core/`, dans un sous-package cohérent avec sa responsabilité.
- Garder les responsabilités séparées; placer la logique métier dans les services seulement lorsqu'elle correspond réellement à ce rôle.
- Utiliser les conventions Java et le style déjà présents dans le fichier ou le package concerné.
- Documenter les types et méthodes publics avec Javadoc, notamment leur contrat et les cas limites utiles aux consommateurs de la bibliothèque.
- Ajouter une dépendance uniquement si elle apporte une valeur nécessaire à la bibliothèque commune; privilégier les dépendances existantes ou le JDK pour les besoins simples.

## Conception et découpage des services

Avant d'implémenter ou d'enrichir un service, analyser son découpage et présenter cette analyse dans la réponse :

- Lister les responsabilités du service (préparation, validation, persistance, notification, calcul...) et vérifier qu'il n'en porte qu'une seule principale.
- Extraire dans un service dédié toute responsabilité réutilisable seule, testable indépendamment ou qui rend la classe difficile à lire (règles, boucles d'agrégation, gestion d'erreurs propres).
- Faire collaborer les services par composition : injection du service délégué par le constructeur, avec une implémentation par défaut neutre si cela simplifie l'usage.
- Ne pas extraire une responsabilité triviale ou à usage unique; justifier brièvement ce qui reste dans le service.
- Tester chaque service séparément, puis l'intégration entre services dans le test du service appelant.

Exemple : `CreationService` orchestre la création et délègue la validation à `ValidationService`.

## Tests unitaires obligatoires

Toute nouvelle implémentation ou modification de comportement doit être accompagnée, dans la même demande, de tests unitaires JUnit Jupiter. Ne pas considérer une implémentation comme terminée sans tests associés, sauf impossibilité expliquée explicitement.

- Placer les tests sous `src/test/java/` en reproduisant le package de la classe testée.
- Nommer les classes de test `<NomDeClasse>Test`.
- Vérifier le comportement observable, les cas limites pertinents et les erreurs attendues; éviter les tests qui ne font que répéter l'implémentation.
- Pour un correctif, ajouter un test qui échoue avant le correctif lorsque cela est possible.
- Exécuter `mvn test` après les changements de code et de tests. Exécuter `mvn clean package` pour la validation complète avant de conclure.
- Si les tests ne peuvent pas être exécutés, indiquer pourquoi et ne pas les présenter comme validés.

## Documentation du dépôt

- Présentation et usage du dépôt : [README.md](README.md).
- Documentation publiée : [docs/index.md](docs/index.md).
- Consulter la documentation existante avant de la modifier et la mettre à jour lorsque le comportement public ou les instructions d'utilisation changent.