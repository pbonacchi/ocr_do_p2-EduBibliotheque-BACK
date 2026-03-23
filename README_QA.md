# README QA - Etudiant Backend

## Contexte et objectif

Ce document formalise le plan de tests et le rapport de couverture pour le backend `etudiant-backend`, sur la base des tests unitaires de `UserService` et des tests d'intégration de `UserController`.

Objectif principal : vérifier la qualité fonctionnelle et technique du socle utilisateur (authentification, inscription, CRUD étudiant), puis piloter l'atteinte du critère projet de couverture >= 80 %.

## Périmètre (scope)

### Ce qui est testé

- Tests unitaires service :
  - `com.openclassrooms.etudiant.service.UserService`
  - logique métier en isolation avec mocks (`UserRepository`, `PasswordEncoder`, `JwtService`, `UserDtoMapper`)
- Tests d'intégration controller :
  - `com.openclassrooms.etudiant.controller.UserController`
  - chaîne complète `Controller -> Service -> Repository -> MySQL`
  - sécurité HTTP (authentifié/non authentifié), validation DTO, gestion des erreurs métier

### Ce qui n'est pas (ou partiellement) testé

- Couverture faible des classes sécurité infra :
  - `JwtAuthenticationFilter`
  - `CustomUserDetailService`
  - scénarios branches de `SpringSecurityConfig`
- Couverture partielle de `RestExceptionHandler` et `JwtService`
- Entité `User` non couverte (méthodes `UserDetails`, getters/setters)
- Hors périmètre :
  - performance/load tests
  - tests E2E front-back
  - résilience/chaos

## Objectifs de test

- Valider la conformité fonctionnelle des opérations utilisateur
- Vérifier les règles métier (unicité login, existence id, validation des champs)
- Vérifier l'intégration entre composants applicatifs et la base
- Vérifier la gestion des erreurs et codes HTTP attendus
- Limiter les régressions sur les parcours critiques

## Stratégie de test

### 1) Tests unitaires (service)

- Isolation complète via mocks (Mockito)
- Focus sur la logique métier pure (`UserService`)
- Frameworks/outils : JUnit 5, Mockito, AssertJ
- Vérification des interactions (`verify`) et exceptions fonctionnelles

### 2) Tests d'intégration (controller)

- `@SpringBootTest` + `@AutoConfigureMockMvc`
- Tests HTTP réels via `MockMvc`
- Base MySQL réelle via Testcontainers (Docker requis)
- Peu/pas de mocks applicatifs (comportement proche production)
- Validation de la persistance en base après appels API

## Environnement de test

- OS execution : Linux
- JDK : 21
- Build : Maven
- Framework : Spring Boot 3.5.5
- Base de données de test : MySQL (Testcontainers 2.0.3)
- Sécurité : Spring Security + JWT
- Couverture : JaCoCo 0.8.12

## Données de test

- Jeux valides :
  - utilisateurs complets (`firstName`, `lastName`, `login`, `password`)
  - étudiants valides pour create/update
- Jeux invalides / erreurs :
  - champs manquants, payload vides
  - login déjà existant
  - id inexistant
  - mot de passe invalide
  - requêtes non authentifiées
- Données limites :
  - `null` (tests unitaires)
  - DTO vides (tests intégration)

## Cas de test (vue synthétique)

Schéma d'identifiants retenu :
- `UT-US-*` pour unit tests `UserService`
- `IT-UC-*` pour integration tests `UserController`

| ID | Type | Description | Préconditions | Entrées | Résultat attendu |
|---|---|---|---|---|---|
| UT-US-REG-01 | Unitaire | register succès | login absent | user valide | save exécuté |
| UT-US-REG-02 | Unitaire | register user null | aucune | `null` | `IllegalArgumentException` |
| UT-US-REG-03 | Unitaire | register login existant | login présent | user doublon | `IllegalArgumentException` |
| UT-US-LOG-01 | Unitaire | login succès | user présent + password OK | login/password valides | token retourné |
| UT-US-LOG-02..05 | Unitaire | login erreurs | selon cas | null / invalide | exceptions attendues |
| UT-US-CRT-01..03 | Unitaire | createStudent nominal + erreurs | selon cas | user valide/null/doublon | save ou exception |
| UT-US-GETALL-01 | Unitaire | getAllStudents | liste mockée | N/A | liste retournée |
| UT-US-GETID-01..02 | Unitaire | getById nominal + absent | selon cas | id | Optional ou exception |
| UT-US-UPD-01..03 | Unitaire | update nominal + erreurs | selon cas | dto/id | save ou exception |
| UT-US-DEL-01..02 | Unitaire | delete nominal + absent | selon cas | id | delete ou exception |
| IT-UC-REG-01..03 | Intégration | POST /api/register | app + DB | DTO invalide/valide/doublon | 400/201/400 |
| IT-UC-LOG-01..03 | Intégration | POST /api/login | user en DB selon cas | creds valides/invalides | 200 ou 400 |
| IT-UC-CRT-01..03 | Intégration | POST /api/students | auth selon cas | studentDTO | 201/401/400 |
| IT-UC-GETALL-01..02 | Intégration | GET /api/students | auth selon cas | N/A | 200/401 |
| IT-UC-GETID-01..03 | Intégration | GET /api/students/{id} | auth + id selon cas | id | 200/400/401 |
| IT-UC-UPD-01..04 | Intégration | PUT /api/students/{id} | auth + id selon cas | dto | 200/400/401 |
| IT-UC-DEL-01..03 | Intégration | DELETE /api/students/{id} | auth + id selon cas | id | 204/400/401 |

## Outils utilisés

- JUnit 5
- Mockito
- AssertJ
- Spring Boot Test / MockMvc
- Spring Security Test (`@WithMockUser`)
- Testcontainers (MySQL)
- Maven Surefire
- JaCoCo

## Rapport de couverture

Commande de génération :

```bash
mvn org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report
```

Rapport HTML :
- `target/site/jacoco/index.html`

Résultats globaux :
- Couverture instructions : **78 %** (671/859)
- Couverture branches : **69 %** (18/26)
- Couverture lignes : **161/212**

Résultats de tests :
- `UserServiceTest` : **19 tests**, 0 échec
- `UserControllerTest` : **21 tests**, 0 échec
- Total : **40 tests**, 100 % passés

Couverture par package (principaux) :
- `com.openclassrooms.etudiant.controller` : **100 %**
- `com.openclassrooms.etudiant.service` : **89 %**
- `com.openclassrooms.etudiant.mapper` : **93 %**
- `com.openclassrooms.etudiant.configuration.security` : **63 %**
- `com.openclassrooms.etudiant.handler` : **51 %**
- `com.openclassrooms.etudiant.entities` : **0 %**

## Critères d'acceptation

- Tous les tests exécutés sans échec
- Aucune régression critique sur parcours login/register/CRUD
- Rapport de couverture généré et archivable
- Seuil projet : **>= 80 %** de couverture globale

État actuel vs seuil :
- **78 % < 80 %** -> critère global non atteint à ce stade

## Critères d'entrée / sortie

### Entrée

- Code compilable
- Environnement Docker actif
- Dépendances Maven résolues

### Sortie

- Tests unitaires + intégration exécutés
- Rapports Surefire et JaCoCo générés
- Défauts critiques analysés et traités/priorisés

