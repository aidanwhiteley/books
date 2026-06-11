# Agent Guide for `books`
## Project overview
- Java 21 / Maven / Spring Boot 4.0.5 application.
- Main app class: `src/main/java/com/aidanwhiteley/books/BooksApplication.java`.
- Purpose: a Spring-based "Cloudy Bookclub" application with MVC + Thymeleaf/HTMX UI, JSON APIs, MongoDB persistence, OAuth2 login, JWT-based auth, RSS feeds, and some service-discovery support.
- The current task focus for most changes should be local build/test success. Do **not** update CI or Docker unless explicitly asked.
## Core stack
- Spring Boot `4.0.5`
- Spring Cloud `2025.1.1`
- Java `21`
- Maven Wrapper (`mvnw.cmd` on Windows)
- Spring MVC + Thymeleaf + HTMX
- Spring Security / OAuth2 client / JWT
- Spring Data MongoDB
- `spring-boot-starter-restclient` for imperative HTTP clients
- `spring-boot-starter-webflux` is present and **must not be removed** — `GoogleBooksDaoAsync` uses `WebClient` for async Google Books API lookups
- Virtual threads are enabled via `spring.threads.virtual.enabled=true` in `application.yml`
- WireMock, GreenMail, Gatling, JUnit 5 for tests
## Key repository conventions
- Prefer **small, targeted changes**.
- Preserve existing code style and public APIs unless the task requires otherwise.
- Read surrounding code before changing behavior.
- When upgrading framework code, follow Spring Boot 4 migration guidance and prefer current Spring Boot 4 idioms over compatibility shims.
## Run and validation commands
Use Windows-friendly commands from the repo root:
```powershell
.\\mvnw.cmd clean compile
.\\mvnw.cmd test
.\\mvnw.cmd clean compile test package
.\\mvnw.cmd spring-boot:run
```
Optional load test:
```powershell
.\\mvnw.cmd gatling:test
```
Optional mutation test (Pitest; first run is slow — scope `targetClasses` to avoid excessive run times):
```powershell
.\\mvnw.cmd -Ppitest -DwithHistory=true "-DtargetClasses=com.aidanwhiteley.books.service.*" test
```
## Profiles and environment
- Default runtime profile in `src/main/resources/application.yml` is `dev-mongo-java-server-no-auth`.
  - Uses in-memory `mongo-java-server`
  - Auto-auths a dummy admin-style user
  - Intended for local development only
- Surefire is configured to run tests with `dev-mongo-java-server`.
- Do **not** treat the checked-in profiles as production-safe.
- Do **not** commit real OAuth secrets, JWT secrets, or environment-specific credentials.
## Security and frontend constraints
- Keep the project's strict security posture intact.
- HTMX is configured with `allowEval=false` and inline script execution is intentionally restricted.
- Do **not** add inline JavaScript to templates; place JS in the appropriate static JS file instead.
- Be cautious when changing authentication, cookies, JWT handling, XSRF handling, or OAuth2 request serialization.
- CORS is **not supported** — removed in early 2025. Use a reverse proxy or middleware proxy for cross-origin SPA development; do not re-add CORS header configuration.
- Data visibility for API responses is controlled by `LimitDataVisibilityAspect` in `controller/aspect/`; changes to what callers receive must account for this AOP layer.
## Test and integration notes
- Aim to run targeted checks while iterating, then run the full enabled test suite before finishing.
- Some integration tests rely on WireMock assets under `src/test/resources`.
- If changing external HTTP client behavior, review affected tests and stubs together.
- Prefer fixing production code and tests in a compatible way rather than disabling tests.
## Dependency and migration notes
- This repo is already on Spring Boot 4.x conventions.
- Be aware of Spring Boot 4 starter/package changes such as:
  - `spring-boot-starter-aop` -> `spring-boot-starter-aspectj`
  - imperative REST client support via `spring-boot-starter-restclient`
- Jackson 3 and Spring Boot 4 repackaging may require import and API updates.
- When changing HTTP client code, prefer the modern `RestClient` API unless there is a strong repository-specific reason not to. **Note:** `GoogleBooksDaoSync` currently still uses the legacy `RestTemplate` and is pending migration to `RestClient`; `GoogleBooksDaoAsync` uses `WebClient` intentionally for async lookups.
- Spring Boot Admin client (`de.codecentric:spring-boot-admin-starter-client`) has been removed because Spring Boot Admin does not yet support Spring Boot 4.x — do not re-add it.
## Files and areas that deserve extra care
- `pom.xml` for dependency alignment
- `src/main/resources/application*.yml` for profile-specific behavior
- `src/main/java/com/aidanwhiteley/books/controller/config/` for security and framework configuration
- `src/main/java/com/aidanwhiteley/books/controller/aspect/` for `LimitDataVisibilityAspect` — AOP-based role-sensitive data stripping applied to controller responses
- `src/main/java/com/aidanwhiteley/books/util/HtmlSanitiserUtils.java` and `ClientInputSanitiserUtils.java` — all user-supplied or external (Google Books API) content must pass through these before persistence or rendering
- `src/main/resources/templates/` and `src/main/resources/static/` for CSP/HTMX-safe frontend changes
- HTMX-specific controllers (`BookControllerHtmx`, `BookSecureControllerHtmx`) are separate from the JSON API controllers (`BookController`, `BookSecureController`) — keep them distinct
- `src/test/` and `src/test/resources/` for integration coverage and WireMock mappings
## Agent workflow expectations
1. Read the relevant code and nearby configuration first.
2. Trace impacted symbols/usages before changing shared behavior.
3. Make the minimum coherent change.
4. Validate edited files.
5. Run targeted tests where practical.
6. Before finishing, run the full currently enabled test suite if the task affects application behavior broadly.
7. Summarize what changed, any follow-up risks, and what was validated.
## Avoid unless explicitly requested
- CI workflow changes
- Docker / `docker-compose.yaml` changes
- Production secret/config rewrites
- Large-scale refactors unrelated to the task
