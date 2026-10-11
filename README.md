# TP 5 — Requêtes avancées, recherche multicritère et pagination

Solution du laboratoire **148** du cours [Hibernate & JPA](https://mliaedu.toubkalit.com/student/courses/28). L'application trouve les salles disponibles sur un créneau, compose des filtres Criteria et retourne des pages avec un comptage cohérent.

## Exécuter

Prérequis : **JDK 17**, **Maven 3.9+** et accès à Maven Central au premier lancement.

```sh
mvn clean verify
mvn exec:java
```

La classe principale est `com.example.App`. Le projet conserve les versions de l'énoncé : `javax.persistence-api:2.2`, `hibernate-core:5.4.30.Final`, `h2:1.4.200`. Les tests utilisent JUnit 5. La base H2 est embarquée et ne nécessite aucune installation externe.

## Couverture de l'énoncé

| Exigence | Réalisation |
|---|---|
| Maven et configuration JPA/Hibernate/H2 | `pom.xml`, unité `SallePU`, déclaration explicite des classes |
| `com.example.model.Salle` | Nom unique, capacité obligatoire, relation `OneToMany` lazy avec cascade |
| `com.example.model.Reservation` | Relation `ManyToOne` lazy, clé `salle_id` obligatoire, dates `debut`/`fin` |
| JPQL : salles disponibles | `SalleRepositoryImpl.findAvailableRooms`, sous-requête `NOT IN` paramétrée |
| Criteria dynamique | Nom partiel insensible à la casse, capacité minimale et disponibilité, tous optionnels |
| Pagination serveur | `setFirstResult((page - 1) * size)` et `setMaxResults(size)`, pages numérotées dès 1 |
| Comptage avec mêmes filtres | `countRooms` reconstruit les prédicats et la sous-requête sur sa propre racine |
| Repository et service | Interfaces `SalleRepository`/`SalleService` et leurs implémentations dans les packages prescrits |
| Résultat générique | `com.example.util.PaginationResult<T>` : éléments, page, taille, total et nombre de pages |
| Programme Java SE de validation | Injection explicite via `setEntityManager` comme dans l'étape 11 ; `DemoData` charge un jeu déterministe |

L'exécutable suit le mode Java SE de l'énoncé : les dépendances sont injectées manuellement. Dans un serveur JEE, on peut remplacer cette construction par CDI/EJB. Le projet utilise `create-drop` à la place du `update` proposé afin que chaque exécution parte du même jeu de données. Les transactions d'insertion sont annulées en cas d'erreur et toutes les ressources sont fermées.

## Règle de disponibilité

Les créneaux sont des intervalles **[début, fin[**. Deux réservations se chevauchent si `débutRecherche < finReservation` **et** `finRecherche > débutReservation`. Une réservation qui termine exactement au début demandé ou commence exactement à la fin demandée ne bloque donc pas la salle. Les salles sans réservation restent disponibles.

La recherche rejette les créneaux inversés ou vides, une seule borne fournie, les capacités négatives, les pages/taille inférieures à 1 et les offsets hors capacité `int`. Les caractères `%` et `_` du nom sont traités comme des caractères littéraux. Les résultats sont ordonnés par nom puis identifiant pour stabiliser les pages.

Les prédicats Criteria ne sont jamais partagés entre deux racines : la requête de comptage les reconstruit avec les mêmes valeurs de filtres. Cela corrige le fragment indicatif de l'étape 7, qui réutilisait les prédicats de la requête de sélection.

## Résultats réellement obtenus

Le jeu contient **6 salles et 5 réservations**, pour une recherche le **1er juin 2024 de 09:00 à 11:00**. La requête JPQL retourne `Boreal(20)`, `Cedre(50)`, `Dune(12)` et `Falaise(35)`. `Atlas` et `Etoile` sont exclues car leurs réservations se chevauchent avec le créneau.

Le filtre `nom='e'`, `capacité >= 20`, avec disponibilité, retourne **Boreal, Cedre, Falaise**. La pagination avec une taille de **2** retourne **Boreal/Cedre en page 1**, **Falaise en page 2**, un total de **3 éléments sur 2 pages**. La page 3 est vide tout en conservant le total filtré.

**9 tests réussis, 0 échec, 0 erreur, 0 ignoré.** Ils vérifient chevauchements, bornes adjacentes, absence de réservation, filtres combinés, comptage, casse et caractères littéraux, pages stables sans doublons, pages vides, validations et relation persistée par cascade.

Les [logs de démonstration](evidence/demo-output.txt), les [logs de tests](evidence/test-output.txt) et les [rapports JUnit](evidence/junit/) sont conservés. Les images sont des captures dans un navigateur de rapports issus des sorties enregistrées ; elles ne sont pas des captures directes du terminal.

![Résultats de la démonstration](screenshots/results.png)

![Résultats des tests](screenshots/tests.png)

Le [rapport des résultats](evidence/report.html) s'ouvre dans un navigateur. L'interface web, les équipements et le tri dynamique évoqués comme prochaines étapes de l'énoncé restent des pistes facultatives ; les fonctionnalités des étapes 1 à 12 sont couvertes ici.

Référence technique consultée : [guide officiel Hibernate 5.4 — Criteria](https://docs.hibernate.org/orm/5.4/userguide/html_single/#criteria).

## Régénérer le rapport et les captures

Les images sont des captures navigateur de résultats réellement exécutés, conservés dans `evidence/`. Les tests proviennent des XML Surefire, également archivés dans `evidence/junit/`. Les empreintes SHA-256 des sources figurent dans `evidence/checksums.json`.

Avec Python 3 et Node.js disponibles, depuis la racine du dépôt :

```sh
mvn clean verify > evidence/test-output.txt 2>&1
mvn exec:java > evidence/demo-output.txt 2>&1
python scripts/generate_report.py
npm install --no-save playwright
npx playwright install chromium
node scripts/capture-results.cjs
```

Le rapport se consulte aussi directement dans `evidence/report.html`. Les captures générées sont `screenshots/results.png` et `screenshots/tests.png`.
