# Instructions pour les agents

## Objectif du projet

Ce dépôt contient une bibliothèque Java commune destinée à être réutilisée dans plusieurs projets. Privilégier une API publique claire, minimale et stable. Éviter les dépendances propres à une application cliente et préserver la compatibilité des API existantes, sauf demande explicite.

## Environnement et commandes

- Java cible : 25, configuré dans `pom.xml`.
- Build : Maven, sans wrapper présent dans le dépôt.
- Exécuter les tests unitaires : `mvn test`.
- Produire une mesure propre de couverture JaCoCo : `mvn clean test`; consulter `target/site/jacoco/index.html`.
- Analyser le style des sources et des tests : `mvn checkstyle:check`.
- Vérifier le build complet avant de terminer une implémentation : `mvn clean package`.
- Générer le site de documentation avec la couverture si la demande le concerne : `mvn clean test site`. `mvn clean site` seul ne collecte pas de couverture.

Ne pas déclarer une vérification réussie si la commande n'a pas été exécutée. Si une commande ne peut pas être lancée, le signaler clairement.

## Implémentation Java

- Placer le code sous `src/main/java/fr/tiogars/core/`, dans un package de domaine (voir « Organisation des packages par domaine »).
- Garder les responsabilités séparées; placer la logique métier dans les services seulement lorsqu'elle correspond réellement à ce rôle.
- Utiliser les conventions Java et le style déjà présents dans le fichier ou le package concerné.
- Documenter les types et méthodes publics avec Javadoc, notamment leur contrat et les cas limites utiles aux consommateurs de la bibliothèque.
- À chaque itération qui ajoute ou modifie du code, commenter chaque membre privé ajouté ou modifié avec une explication concise de son rôle.
- Ajouter une dépendance uniquement si elle apporte une valeur nécessaire à la bibliothèque commune; privilégier les dépendances existantes ou le JDK pour les besoins simples.

## Conception et découpage des services

Avant d'implémenter ou d'enrichir un service, analyser son découpage et présenter cette analyse dans la réponse :

- Lister les responsabilités du service (préparation, validation, persistance, notification, calcul...) et vérifier qu'il n'en porte qu'une seule principale.
- Extraire dans un service dédié toute responsabilité réutilisable seule, testable indépendamment ou qui rend la classe difficile à lire (règles, boucles d'agrégation, gestion d'erreurs propres).
- Faire collaborer les services par composition : injection du service délégué par le constructeur, avec une implémentation par défaut neutre si cela simplifie l'usage.
- Ne pas extraire une responsabilité triviale ou à usage unique; justifier brièvement ce qui reste dans le service.
- Tester chaque service séparément, puis l'intégration entre services dans le test du service appelant.

Exemple : `CreationService` orchestre la création et délègue la validation à `ValidationService`.

## Organisation des packages par domaine

Pour chaque nouvelle demande, se demander dans quel domaine chaque type doit vivre et présenter ce choix dans la réponse :

- Regrouper d'abord les types par domaine fonctionnel (`fr.tiogars.core.<domaine>`), puis par rôle dans les sous-packages transversaux répertoriés ci-dessous (`services`, `exceptions`...). Ne pas créer de package technique global regroupant plusieurs domaines.
- Un service qui peut être appelé indépendamment des autres a son propre domaine, même s'il est aujourd'hui utilisé par un seul appelant.
- Placer chaque service dans `fr.tiogars.core.<domaine>.services` et chaque exception dans `fr.tiogars.core.<domaine>.exceptions`. Appliquer cette convention à toute nouvelle classe et à tout déplacement demandé.
- Garder les autres types dans leur domaine; utiliser un sous-package transversal seulement si son rôle est défini dans le registre ci-dessous.
- Les dépendances entre domaines vont dans un seul sens : un domaine d'orchestration peut dépendre d'un domaine plus général, jamais l'inverse ni de manière circulaire.
- Lors d'un déplacement de package, mettre à jour les tests (même package), la documentation et signaler la rupture de compatibilité pour les consommateurs.

Exemple : `fr.tiogars.core.creation.services.CreationService` utilise `fr.tiogars.core.creation.exceptions.CreationException` et délègue la validation au domaine autonome `validation`.

### Registre des sous-packages transversaux

Ces noms de sous-packages sont réutilisables dans chaque domaine; ils ne constituent pas des domaines fonctionnels autonomes.

| Sous-package | Rôle | Exemple |
| --- | --- | --- |
| `services` | Services et orchestration propres au domaine | `fr.tiogars.core.creation.services.CreationService` |
| `exceptions` | Exceptions propres au domaine | `fr.tiogars.core.creation.exceptions.CreationException` |

- Consulter ce registre avant de choisir le package d'un type lors des prochaines demandes.
- Ajouter une entrée avec son rôle et un exemple dès qu'un nouveau sous-package transversal devient nécessaire; réutiliser les noms existants pour un même rôle.
- Ne pas créer de sous-package vide par anticipation ni déplacer des classes hors du périmètre demandé uniquement pour appliquer la convention.

## Tests unitaires obligatoires

Toute nouvelle implémentation ou modification de comportement doit être accompagnée, dans la même demande, de tests unitaires JUnit Jupiter. Ne pas considérer une implémentation comme terminée sans tests associés, sauf impossibilité expliquée explicitement.

- Placer les tests sous `src/test/java/` en reproduisant le package de la classe testée.
- Nommer les classes de test `<NomDeClasse>Test`.
- À chaque itération, documenter dans le code tous les éléments de test ajoutés ou modifiés, quelle que soit leur visibilité : packages, classes (y compris imbriquées), records et leurs composants, champs, constructeurs, méthodes de test, méthodes de cycle de vie, méthodes auxiliaires et redéfinitions dans les doublures ou classes anonymes. Utiliser une Javadoc concise décrivant le rôle de chaque élément et, pour chaque scénario, les conditions exercées et le résultat ou l'erreur attendu; documenter les paramètres et retours utiles. Maintenir ces commentaires en cohérence avec les assertions et le comportement réellement vérifié. Les noms explicites et les annotations JUnit ne remplacent pas ces commentaires; commenter aussi les blocs non évidents sans paraphraser chaque instruction.
- Vérifier le comportement observable, les cas limites pertinents et les erreurs attendues; éviter les tests qui ne font que répéter l'implémentation.
- Pour un correctif, ajouter un test qui échoue avant le correctif lorsque cela est possible.
- À chaque itération qui ajoute ou modifie du code, augmenter la couverture des comportements du périmètre touché en testant les nouveaux chemins, les cas limites et les branches d'erreur pertinentes; ne pas ajouter de tests sans valeur uniquement pour gonfler un pourcentage.
- À chaque itération qui ajoute ou modifie du code, exécuter `mvn checkstyle:check` et corriger toutes les violations signalées avant de poursuivre; l'analyse couvre les sources de production et de test.
- À chaque itération qui ajoute ou modifie du code ou des tests, exécuter `mvn clean test` avant les changements pour relever la couverture JaCoCo des classes concernées (lignes et branches couvertes et manquées); conserver cette référence hors de `target`, supprimé par `clean`. Après les changements, régénérer le rapport avec la même commande et comparer les compteurs dans `target/site/jacoco/index.html` ou `target/site/jacoco/jacoco.xml`. Couvrir les nouveaux chemins et corriger toute régression non justifiée; expliquer explicitement les exceptions dans le bilan. Si le code a déjà changé avant le relevé initial, signaler l'absence de référence sans annuler les modifications existantes. Ne pas imposer de seuil global arbitraire, exclure du code ou désactiver l'agent pour masquer une baisse. Un rapport absent, périmé ou issu de tests désactivés ne valide pas la couverture. Dans le bilan, indiquer les commandes réellement exécutées, la couverture observée sur le périmètre touché et les limites éventuelles de la comparaison.
- À chaque itération, traiter les alertes SonarQube pertinentes sur le code ajouté ou modifié. Pour une alerte indiquant qu'une lambda de test peut contenir plusieurs appels susceptibles de lever une exception d'exécution (par exemple avec `assertThrows`), préparer les données et effectuer les appels auxiliaires avant la lambda, afin qu'elle ne contienne que l'opération dont l'exception est vérifiée; ne pas supprimer ou masquer l'alerte pour la contourner.
- Corriger toute nouvelle erreur de compilation détectée après une modification avant de conclure; ne pas la laisser de côté au seul motif qu'un autre contrôle passe.
- Si les diagnostics de l'éditeur contredisent le build Maven, vérifier avec un build propre et traiter les diagnostics résiduels comme potentiellement périmés jusqu'à leur actualisation ou confirmation.
- Exécuter `mvn test` après les changements de code et de tests. Exécuter `mvn clean package` pour la validation complète avant de conclure.
- Si les tests ne peuvent pas être exécutés, indiquer pourquoi et ne pas les présenter comme validés.

## Documentation du dépôt

- Présentation et usage du dépôt : [README.md](README.md).
- Documentation publiée : [docs/index.md](docs/index.md).
- Consulter la documentation existante avant de la modifier et la mettre à jour lorsque le comportement public ou les instructions d'utilisation changent.

### Documentation à chaque itération

- À chaque ajout, correction ou évolution, vérifier la documentation des éléments concernés et la mettre à jour dans la même itération. Si aucune modification documentaire n'est nécessaire, le préciser dans le bilan avec une justification.
- Pour chaque type public et point d'extension ajouté ou modifié, indiquer son objectif, son domaine, le besoin auquel il répond et son rôle dans la collaboration entre services.
- Décrire les contrats utiles : entrées, sorties, valeurs par défaut, ordre des opérations, cas limites, erreurs, effets de bord et limites (notamment transactionnelles).
- Donner un exemple concret d'utilisation pour chaque élément, ou le référencer dans un exemple partagé qui montre explicitement son rôle. Inclure les imports, les types et les dépendances nécessaires, ainsi que le résultat attendu; éviter les dépôts ou méthodes fictifs non définis.
- Documenter au moins le parcours nominal et une erreur représentative lorsque l'élément peut échouer. Distinguer les garanties réelles des responsabilités laissées au projet consommateur.
- Centraliser les exemples détaillés dans [docs/index.md](docs/index.md), maintenir un aperçu et un lien depuis [README.md](README.md), et garder les Javadoc cohérentes avec ces documents.
- Vérifier les exemples Java contre l'API actuelle (compilation, puis exécution ou test quand possible), les imports, les liens locaux et les résultats annoncés. Exécuter `mvn clean test site` pour une modification de la documentation publiée, vérifier la présence du rapport JaCoCo dans le site et signaler toute vérification impossible.
- Lors d'un déplacement ou d'une rupture d'API, actualiser les exemples et expliquer les adaptations nécessaires aux consommateurs. Ne pas publier un comportement prévu mais non implémenté comme une fonctionnalité disponible.