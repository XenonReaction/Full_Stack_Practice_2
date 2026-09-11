# BUILD_GUIDE.md — Guestbook, from nothing to running (build 2: tests + CLI-first)

Follow this top to bottom. Every command is meant to be run in the
**Ubuntu (WSL) shell** unless it says otherwise.

**What changed from build 1**

- Target stack is now **Spring Boot 4.1 / Java 21** and **Angular 20+** (what
  `start.spring.io` and `npm i -g @angular/cli` give you today). A few names moved
  — noted inline.
- **Phase 3 onward is "build it yourself".** Each step gives you the *tool*
  (a CLI generator where one exists), the *contract* the file has to satisfy, and
  the *gotchas*. The full source is still here, collapsed under
  **▸ Reference implementation** — open it only after you've tried, and if you
  peek, note it in `RUNBOOK.md`.
- **Unit testing is part of the build now**, not an afterthought: a fast
  Mockito tier, Spring slice tests (`@DataJpaTest`, `@WebMvcTest`), a
  Testcontainers integration tier for the backend; `HttpTestingController` and
  `TestBed` for Angular. Every phase from 3 on ends with green tests.

At the end of each phase there is a **Verify** box. Do not move on until it passes.

- Working folder: `~/workspace/fullstack-practice-2`
- App name: `guestbook`
- Backend port: `8081` · Frontend dev port: `4200` · Full-stack port: `8080` · Postgres: `5432`
- Passcode (default): `let-me-in`

---

## Phase 0 — Prerequisites (first time only, ~60 min)

### 0.0 Base packages (fresh Ubuntu)

```bash
sudo apt update && sudo apt install -y curl zip unzip
```

`zip`/`unzip` are required by SDKMAN in 0.2 — without them its installer aborts.

### 0.1 Docker Desktop

1. Install **Docker Desktop for Windows** (Windows side).
2. Docker Desktop → **Settings → Resources → WSL Integration** → enable for **Ubuntu**. Apply & restart.
3. In the Ubuntu shell:

```bash
docker run --rm hello-world
docker compose version
```

Both must succeed. Docker also has to be **running** later whenever you run the
backend test suite (Testcontainers starts a real Postgres container).

### 0.2 Java 21 + Maven via SDKMAN

```bash
curl -s "https://get.sdkman.io" | bash          # downloads + runs the SDKMAN installer
source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk list java | grep -i tem        # find the current 21.x Temurin id
sdk install java 21.0.5-tem        # use whatever id the line above showed
sdk install maven

java -version                       # -> openjdk version "21..."
mvn -version                        # -> Apache Maven 3.9.x, Java 21
```

Open a new shell afterward so `sdkman-init.sh` loads automatically (SDKMAN adds it
to `~/.bashrc`).

Optional but recommended for this build — the **Spring Boot CLI**, so you can
scaffold the project from the terminal instead of the website:

```bash
sdk install springboot
spring --version
spring init --list | head -40      # shows dependency ids and boot versions
```

### 0.3 Node 22 + Angular CLI via nvm

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
# close and reopen the shell
nvm install 22
nvm alias default 22
node -v                             # v22.x
npm -v

npm install -g @angular/cli
ng version                          # note the major version — this guide is written against 20/21
```

> **Angular version drift.** The CLI ships a new major every 6 months and file
> naming / the test runner have both changed recently. Where this guide says
> `guestbook.ts` your CLI may produce `guestbook.component.ts`; where it says the
> class is `Guestbook` yours may be `GuestbookComponent`. **Run the generator,
> read what it prints, and adjust imports to match.** That mismatch is the point
> of this build.

### 0.4 Editor

From Ubuntu, in the project folder later: `code .` opens VS Code with the WSL
extension attached. Install the "Extension Pack for Java" and "Angular Language
Service" when prompted. The Java pack's **Source Action → Generate Getters and
Setters / Constructors** is used in Phase 3.

> **Verify Phase 0**
> ```bash
> docker run --rm hello-world && java -version && mvn -version && node -v && ng version
> ```
> All five run without "command not found".

---

## Phase 1 — Project skeleton + PostgreSQL (~20 min)

### 1.1 Folders

```bash
mkdir -p ~/workspace/fullstack-practice-2
cd ~/workspace/fullstack-practice-2
mkdir backend frontend
```

### 1.2 `docker-compose.yml` (dev — Postgres only)

Create `~/workspace/fullstack-practice-2/docker-compose.yml`. The credentials come
from `.env` (next step); the `:-guestbook` parts are shell-style fallbacks Compose
applies when the variable is unset.

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: ${DB_NAME:-guestbook}
      POSTGRES_USER: ${DB_USERNAME:-guestbook}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-guestbook}
    ports:
      - "5432:5432"
    volumes:
      - guestbook_pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  guestbook_pgdata:
```

> `$${...}` (double dollar) escapes the variable so **Compose** passes it through
> for the shell **inside** the container to expand, instead of expanding it itself.

> **Credential changes and the volume.** Postgres only creates
> `POSTGRES_USER` / `POSTGRES_PASSWORD` **the first time it starts against an empty
> data directory.** If you edit `.env` after the volume exists, the new role
> silently doesn't get created and the backend fails with
> `role "…" does not exist` or `password authentication failed`. Fix:
> `docker compose down -v` (wipes the volume) then `docker compose up -d`.

### 1.3 `.env.example` and `.env`

`.env` is read automatically by `docker compose`, and by Spring in Phase 2. Commit
`.env.example`, never `.env`.

Create `.env.example`:

```bash
APP_SUBMISSION_PASSCODE=let-me-in
DB_NAME=guestbook
DB_USERNAME=guestbook
DB_PASSWORD=guestbook
DB_URL=jdbc:postgresql://localhost:5432/guestbook
```

```bash
cp .env.example .env
# now edit .env with real values if you want; the defaults work fine for local dev
```

### 1.4 `.gitignore`

```gitignore
# java
backend/target/
# node
frontend/node_modules/
frontend/dist/
frontend/.angular/
# env
.env
# ide
.idea/
.vscode/
*.iml
```

### 1.5 Start the database

```bash
docker compose up -d
docker compose ps          # db is "healthy" after ~10s
```

> **Verify Phase 1**
> ```bash
> docker compose exec db psql -U "$DB_USERNAME" -d "$DB_NAME" -c '\dt'
> ```
> (or `-U guestbook -d guestbook` if you didn't export `.env` in this shell)
> Output: `Did not find any relations.` — an empty database, correct.

---

## Phase 2 — Spring Boot backend scaffold (~45 min)

### 2.1 Generate the project

Either use the **Spring Boot CLI**:

```bash
cd ~/workspace/fullstack-practice-2
spring init \
  --type=maven-project \
  --java-version=21 \
  --group-id=com.example \
  --artifact-id=guestbook \
  --name=guestbook \
  --package-name=com.example.guestbook \
  --dependencies=web,data-jpa,postgresql,validation,actuator,testcontainers \
  --extract backend
ls backend           # pom.xml, src, mvnw, ...
```

…or pick the same dependencies on <https://start.spring.io> (**Spring Web**,
**Spring Data JPA**, **PostgreSQL Driver**, **Validation**, **Spring Boot
Actuator**, **Testcontainers**), download, unzip into `backend/`, and flatten the
inner folder.

> **Boot 4 naming.** "Spring Web" now resolves to the
> `spring-boot-starter-webmvc` starter, and the old single `spring-boot-starter-test`
> is split into per-module test starters (`spring-boot-starter-webmvc-test`,
> `spring-boot-starter-data-jpa-test`, …). Initializr adds the right ones; don't
> hand-add `spring-boot-starter-test`.

> **Already have `backend/`?** You generated it from the website last time. Keep
> it. You only need to *add the Testcontainers dependencies* — do that in 2.6.

### 2.2 `backend/src/main/resources/application.yml`

Delete `application.properties` (or the stray `application.yaml` Initializr leaves)
and create **one** file at exactly this path.

> **Two mistakes from build 1, both fixed by getting this right:**
>
> 1. **Location and name.** Maven only copies `src/main/resources/**` onto the
>    classpath. A file at `src/resources/application.yml` (missing `main/`), or a
>    second `application.yaml` competing with `application.yml`, means your
>    datasource config is simply *not loaded* — the app dies at startup with
>    `Failed to determine a suitable driver class`. After a build, confirm with
>    `ls backend/target/classes` — `application.yml` must be there.
> 2. **Placeholder syntax.** Inside a Spring config file the default goes after a
>    **single colon**: `${DB_USERNAME:guestbook}`. The shell / Compose form
>    `${DB_USERNAME:-guestbook}` (with the dash) is **not** Spring syntax — if you
>    copy it in, the literal `-guestbook` becomes part of the value.

```yaml
server:
  port: 8081

spring:
  config:
    # pull KEY=VALUE lines from the repo-root .env when running ./mvnw from backend/.
    # [.properties] tells Spring how to parse an extensionless file. optional: = no error if absent.
    import: optional:file:../.env[.properties]
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/guestbook}
    username: ${DB_USERNAME:guestbook}
    password: ${DB_PASSWORD:guestbook}
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    show-sql: true
    properties:
      hibernate.format_sql: true

management:
  endpoints:
    web:
      exposure:
        include: health,info

app:
  submission-passcode: ${APP_SUBMISSION_PASSCODE:let-me-in}
  cors-allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:http://localhost:4200}
```

### 2.3 `backend/src/main/resources/application-docker.yml`

Used only when `SPRING_PROFILES_ACTIVE=docker` (Phase 8). Same keys, but the host
is the Compose service name `db`, and there's no `.env` to import (the container
gets real environment variables):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db:5432/guestbook
    username: ${DB_USERNAME:guestbook}
    password: ${DB_PASSWORD:guestbook}
```

### 2.4 First run

```bash
cd backend
./mvnw spring-boot:run
```

Wait for `Started GuestbookApplication`. The `spring.config.import` line means you
do **not** have to `export` anything — Spring reads `../.env` itself. (If you ever
run the built jar from a different directory, either `cd` to the repo root first
or `set -a && source .env && set +a` beforehand.)

> **Verify Phase 2 (run)**
> ```bash
> curl -s localhost:8081/actuator/health      # {"status":"UP"}
> ```
> Stop the app with `Ctrl+C`.

### 2.5 Make the context test pass with Testcontainers

The Initializr project ships one test — `GuestbookApplicationTests.contextLoads()`
— which boots the **whole** application context, datasource included. With no
database reachable it fails (`Failed to determine a suitable driver class`), and
so `./mvnw package` fails. Rather than point it at your dev DB, give the test
suite its own throwaway Postgres via Testcontainers.

**Add to `pom.xml`** (`<dependencies>`), if `spring init` didn't:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

> **Artifact names.** Spring Boot 4.1 aligns with **Testcontainers 2.x** (its BOM
> imports `testcontainers-bom:2.0.5`). Testcontainers 2.0 renamed every module to a
> `testcontainers-*` prefix — `junit-jupiter` → `testcontainers-junit-jupiter`,
> `postgresql` → `testcontainers-postgresql`. Use the old 1.x names and Maven
> fails the POM with *"'dependencies.dependency.version' … is missing"*. No
> `<version>` is needed once the names are right — the BOM supplies `2.0.5`. (This
> is also why the container class moved, below.)

**Create `src/test/java/com/example/guestbook/TestcontainersConfiguration.java`.**
Contract: a `@TestConfiguration` exposing one `@Bean` `PostgreSQLContainer`
(from `org.testcontainers.postgresql` — the `org.testcontainers.containers` one is
deprecated in 2.x) annotated `@ServiceConnection`, so Boot wires the datasource to
it automatically.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;   // Testcontainers 2.x package

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:16");
    }
}
```

> In Testcontainers 2.x `PostgreSQLContainer` lives in
> `org.testcontainers.postgresql` and is no longer self-generic — no `<>`. The old
> `org.testcontainers.containers.PostgreSQLContainer<?>` from 1.x tutorials still
> compiles but is **deprecated** (your IDE will flag it) — use the new package.
> `@ServiceConnection` itself is unchanged; that's still the right mechanism. Its
> Postgres wiring comes from `spring-boot-jdbc`, already on the
> classpath via `spring-boot-starter-data-jpa`.
</details>

**Edit `GuestbookApplicationTests`** to import it:

```java
@Import(TestcontainersConfiguration.class)   // org.springframework.context.annotation.Import
@SpringBootTest
class GuestbookApplicationTests {
    @Test
    void contextLoads() { }
}
```

Optional — a `TestGuestbookApplication` main class lets you run the app locally
against the same throwaway container with `./mvnw spring-boot:test-run`.

```bash
docker info >/dev/null    # daemon must be up
./mvnw test
```

> **Verify Phase 2 (test)**
> `./mvnw test` → `BUILD SUCCESS`, `Tests run: 1`. Run `docker ps` while it's
> going and you'll see a `postgres:16` container appear and vanish.

---

## Phase 3 — Backend domain (~2 h with tests)

All paths are under `backend/src/main/java/com/example/guestbook/` unless noted.
No CLI generates JPA/Spring classes, so the workflow here is: **make the file,
type the contract, let the IDE generate boilerplate (constructors, getters),
write the body.** Reference solutions are collapsed — try first.

### 3.1 Enable config-properties scanning — edit `GuestbookApplication.java`

Add `@ConfigurationPropertiesScan` to the class (next to `@SpringBootApplication`)
and the matching import.

> **Why it matters.** `AppProperties` (3.2) carries only `@ConfigurationProperties`,
> which does **not** register a bean by itself. Something must activate the scan:
> this annotation, or `@EnableConfigurationProperties(AppProperties.class)`, or
> `@Component` on `AppProperties`. Miss it and the app dies at 3.9 with
> `Parameter … required a bean of type '…AppProperties' that could not be found`.
> The `import` line alone does nothing — the annotation has to be on the class.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GuestbookApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuestbookApplication.class, args);
    }
}
```
</details>

### 3.2 `config/AppProperties.java`

**Contract.** `@ConfigurationProperties(prefix = "app")`, two `String` properties:
`submissionPasscode`, `corsAllowedOrigins` (bound from `app.submission-passcode` /
`app.cors-allowed-origins` — Spring relaxes the naming). Plain getters/setters, or
make it a `record` bound with `@ConfigurationProperties` + a canonical constructor.

<details><summary>▸ Reference implementation (class form)</summary>

```java
package com.example.guestbook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Shared secret a submission must include to be accepted. */
    private String submissionPasscode;

    /** Comma-separated origins allowed to call /api/** in dev. */
    private String corsAllowedOrigins = "http://localhost:4200";

    public String getSubmissionPasscode() { return submissionPasscode; }
    public void setSubmissionPasscode(String v) { this.submissionPasscode = v; }

    public String getCorsAllowedOrigins() { return corsAllowedOrigins; }
    public void setCorsAllowedOrigins(String v) { this.corsAllowedOrigins = v; }
}
```
</details>

### 3.3 `config/WebConfig.java` — dev CORS

**Contract.** `@Configuration implements WebMvcConfigurer`; override
`addCorsMappings` to allow the `AppProperties.corsAllowedOrigins` values (split on
`,`) to `GET,POST` `/api/**`. Inject `AppProperties` via the constructor.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public WebConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.getCorsAllowedOrigins().split("\\s*,\\s*"))
                .allowedMethods("GET", "POST");
    }
}
```
</details>

### 3.4 `message/Message.java` — JPA entity

**Contract.**

- `@Entity @Table(name = "messages")`
- `Long id` — `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`
- `String name` — `@Column(nullable = false, length = 100)`
- `String body` — `@Column(nullable = false, length = 2000)`
- `OffsetDateTime createdAt` — `@Column(name = "created_at", nullable = false)`
- a `protected` no-arg constructor (JPA needs it), a public all-args constructor
  you call, getters only (no setters — an entry doesn't change once posted).

Type the fields, then **IDE → Generate Constructor / Getters**.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Message() { /* for JPA */ }

    public Message(String name, String body, OffsetDateTime createdAt) {
        this.name = name;
        this.body = body;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBody() { return body; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
```
</details>

### 3.5 `message/MessageRepository.java`

**Contract.** `interface MessageRepository extends JpaRepository<Message, Long>`
with one **derived query method** — Spring Data writes the implementation from the
name — returning all rows newest-first: `findAllByOrderByCreatedAtDesc()`.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.message;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findAllByOrderByCreatedAtDesc();
}
```
</details>

### 3.6 `message/CreateMessageRequest.java` — inbound DTO

**Contract.** A `record` with three components, each Jakarta-validated:
`name` `@NotBlank @Size(max = 100)`, `message` `@NotBlank @Size(max = 2000)`,
`passcode` `@NotBlank`.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 2000) String message,
        @NotBlank String passcode) {
}
```
</details>

### 3.7 `message/MessageResponse.java` — outbound DTO

**Contract.** A `record` `(Long id, String name, String message, OffsetDateTime
createdAt)` — **no passcode field**. A static factory `from(Message)` maps entity →
response (note the entity's field is `body`, the DTO's is `message`).

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.message;

import java.time.OffsetDateTime;

public record MessageResponse(Long id, String name, String message, OffsetDateTime createdAt) {

    static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getName(), m.getBody(), m.getCreatedAt());
    }
}
```
</details>

### 3.8 `message/InvalidPasscodeException.java`

**Contract.** `extends RuntimeException`, no-arg constructor calling
`super("Invalid passcode")`.

### 3.9 `message/MessageService.java`

**Contract.**

- `@Service`, constructor-injects `MessageRepository` + `AppProperties`.
- `findAll()` — `@Transactional(readOnly = true)`, returns
  `List<MessageResponse>`, newest-first, mapped via `MessageResponse::from`.
- `create(CreateMessageRequest)` — `@Transactional`; if the request passcode
  doesn't `equals` `AppProperties.getSubmissionPasscode()`, throw
  `InvalidPasscodeException`; otherwise save a `Message` with `name`/`message`
  **trimmed** and `createdAt = OffsetDateTime.now()`, return `MessageResponse.from`
  the saved entity.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.message;

import com.example.guestbook.config.AppProperties;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private final MessageRepository repository;
    private final AppProperties appProperties;

    public MessageService(MessageRepository repository, AppProperties appProperties) {
        this.repository = repository;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse create(CreateMessageRequest request) {
        if (!appProperties.getSubmissionPasscode().equals(request.passcode())) {
            throw new InvalidPasscodeException();
        }
        Message saved = repository.save(
                new Message(request.name().trim(), request.message().trim(), OffsetDateTime.now()));
        return MessageResponse.from(saved);
    }
}
```
</details>

### 3.10 `message/MessageController.java`

**Contract.** `@RestController @RequestMapping("/api/messages")`, constructor-injects
`MessageService`.

- `GET` → `List<MessageResponse>` from `service.findAll()`.
- `POST` → `@ResponseStatus(HttpStatus.CREATED)`, body
  `@Valid @RequestBody CreateMessageRequest`, returns `service.create(...)`.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.message;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    @GetMapping
    public List<MessageResponse> list() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse create(@Valid @RequestBody CreateMessageRequest request) {
        return service.create(request);
    }
}
```
</details>

### 3.11 `error/ApiExceptionHandler.java`

**Contract.** `@RestControllerAdvice`. Two handlers returning RFC-7807
`ProblemDetail`:

- `InvalidPasscodeException` → `403 FORBIDDEN`, detail = `ex.getMessage()`.
- `MethodArgumentNotValidException` → `400 BAD_REQUEST`, detail = the field errors
  joined as `"field message; field message"`.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.error;

import com.example.guestbook.message.InvalidPasscodeException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidPasscodeException.class)
    public ProblemDetail handlePasscode(InvalidPasscodeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
```
</details>

### 3.12 Run it

```bash
cd backend
./mvnw spring-boot:run
```

Log shows `Started GuestbookApplication` and a Hibernate `create table messages …`.
If instead you get `APPLICATION FAILED TO START` / "required a bean of type
`…AppProperties`", 3.1 is missing its annotation.

> **Verify Phase 3 (run)**
> ```bash
> docker compose exec db psql -U guestbook -d guestbook -c '\d messages'
> ```
> Columns `id, name, body, created_at`. Stop the app.

---

### Backend unit tests

Three tiers, fastest first. All live under
`backend/src/test/java/com/example/guestbook/`.

| Tier | Annotation | Spins up | Use for |
|---|---|---|---|
| unit | none (`@ExtendWith(MockitoExtension.class)`) | nothing | pure logic — the passcode check, trimming, mapping |
| slice | `@DataJpaTest`, `@WebMvcTest` | one layer | the derived query; request→JSON, validation, status codes |
| integration | `@SpringBootTest` | whole app + Testcontainers PG | wiring, `contextLoads` |

### 3.13 `message/MessageServiceTest.java` — pure unit

**Contract.** `@ExtendWith(MockitoExtension.class)`; `@Mock MessageRepository`; a
real `AppProperties` with the passcode set in `@BeforeEach`; `new
MessageService(...)`. Cases:

- wrong passcode → `assertThatThrownBy(...).isInstanceOf(InvalidPasscodeException.class)`
  **and** `verify(repository, never()).save(any())`.
- good passcode → capture the saved `Message` (`ArgumentCaptor`), assert `name` /
  `body` were trimmed.
- `findAll()` → stub `findAllByOrderByCreatedAtDesc()`, assert the mapping to
  `MessageResponse`.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.guestbook.config.AppProperties;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    MessageRepository repository;

    AppProperties appProperties;
    MessageService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setSubmissionPasscode("let-me-in");
        service = new MessageService(repository, appProperties);
    }

    @Test
    void create_rejects_a_wrong_passcode() {
        var request = new CreateMessageRequest("Ada", "hello", "wrong");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(InvalidPasscodeException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_trims_name_and_body() {
        when(repository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(new CreateMessageRequest("  Ada  ", "  hello  ", "let-me-in"));

        var saved = ArgumentCaptor.forClass(Message.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Ada");
        assertThat(saved.getValue().getBody()).isEqualTo("hello");
    }

    @Test
    void findAll_maps_entities_to_responses() {
        when(repository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(new Message("Ada", "first", OffsetDateTime.now())));

        List<MessageResponse> result = service.findAll();

        assertThat(result).singleElement()
                .satisfies(r -> assertThat(r.name()).isEqualTo("Ada"));
    }
}
```
</details>

### 3.14 `message/MessageRepositoryTest.java` — `@DataJpaTest` slice

**Contract.** `@DataJpaTest` +
`@AutoConfigureTestDatabase(replace = Replace.NONE)` +
`@Import(TestcontainersConfiguration.class)` so the slice talks to the real
Postgres container, not an in-memory DB. Save an older and a newer `Message`,
assert `findAllByOrderByCreatedAtDesc()` returns newer-then-older.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.guestbook.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MessageRepositoryTest {

    @Autowired
    MessageRepository repository;

    @Test
    void findAllByOrderByCreatedAtDesc_returns_newest_first() {
        repository.save(new Message("Ada", "older", OffsetDateTime.now().minusHours(1)));
        repository.save(new Message("Grace", "newer", OffsetDateTime.now()));

        assertThat(repository.findAllByOrderByCreatedAtDesc())
                .extracting(Message::getName)
                .containsExactly("Grace", "Ada");
    }
}
```
</details>

### 3.15 `message/MessageControllerTest.java` — `@WebMvcTest` slice

**Contract.** `@WebMvcTest(MessageController.class)` (loads MVC + your
`@RestControllerAdvice`, nothing else); `@Autowired MockMvc`;
`@MockitoBean MessageService` (Boot 4 removed `@MockBean` — use `@MockitoBean`
from `org.springframework.test.context.bean.override.mockito`). Cases:

- `GET` → stub `findAll()`, expect `200`, `$[0].name`, and `$[0].passcode`
  **does not exist**.
- `POST` blank name → expect `400` (validation fires before the service).
- `POST` with `service.create` stubbed to throw `InvalidPasscodeException` →
  expect `403`.

<details><summary>▸ Reference implementation</summary>

```java
package com.example.guestbook.message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MessageService service;

    @Test
    void GET_lists_messages_without_the_passcode() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new MessageResponse(1L, "Ada", "hi", OffsetDateTime.parse("2026-01-01T00:00:00Z"))));

        mvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ada"))
                .andExpect(jsonPath("$[0].passcode").doesNotExist());
    }

    @Test
    void POST_with_a_blank_name_is_400() throws Exception {
        mvc.perform(post("/api/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","message":"hi","passcode":"let-me-in"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_with_a_wrong_passcode_is_403() throws Exception {
        when(service.create(any())).thenThrow(new InvalidPasscodeException());

        mvc.perform(post("/api/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","message":"hi","passcode":"nope"}"""))
                .andExpect(status().isForbidden());
    }
}
```
</details>

### 3.16 Run the whole backend suite

```bash
cd backend
./mvnw test          # Docker must be running for 3.14 + contextLoads
```

> **Verify Phase 3 (test)**
> `BUILD SUCCESS`. `MessageServiceTest`, `MessageRepositoryTest`,
> `MessageControllerTest`, `GuestbookApplicationTests` all green. Failures here
> should point at *your code*, not the test — read the first assertion or the
> first `Caused by:`.

---

## Phase 4 — Manual API check with curl (~15 min)

The tests cover the logic; this confirms the process actually serves HTTP. With
`docker compose up -d` and `./mvnw spring-boot:run` both active:

```bash
# 1. valid submission -> 201, body has id + createdAt, no passcode
curl -i -X POST localhost:8081/api/messages -H 'Content-Type: application/json' \
  -d '{"name":"Ada","message":"First entry","passcode":"let-me-in"}'

# 2. wrong passcode -> 403
curl -i -X POST localhost:8081/api/messages -H 'Content-Type: application/json' \
  -d '{"name":"Mallory","message":"sneaky","passcode":"wrong"}'

# 3. blank fields -> 400
curl -i -X POST localhost:8081/api/messages -H 'Content-Type: application/json' \
  -d '{"name":"","message":"","passcode":"let-me-in"}'

# 4. list -> 200, newest first
curl -s localhost:8081/api/messages
```

> **Verify Phase 4**
> #1 → `201` + JSON with `id`/`createdAt`, no `passcode`. #2 → `403`. #3 → `400`.
> #4 lists the "Ada" row.
> ```bash
> docker compose exec db psql -U guestbook -d guestbook -c 'select id, name, body from messages;'
> ```
> One row. Stop the backend.

---

## Phase 5 — Angular frontend scaffold (~30 min)

### 5.1 Generate

```bash
cd ~/workspace/fullstack-practice-2
rm -rf frontend
ng new frontend --style=css --ssr=false
# Accept defaults for other prompts (zoneless: either is fine; analytics: your call).
cd frontend
```

### 5.2 Register HttpClient — `src/app/app.config.ts`

Add `provideHttpClient()` to the `providers` array, keeping everything the CLI
generated.

```typescript
import { provideHttpClient } from '@angular/common/http';
// ...
export const appConfig: ApplicationConfig = {
  providers: [
    /* ...whatever the CLI generated, untouched... */
    provideHttpClient(),
  ],
};
```

### 5.3 Dev proxy — `frontend/proxy.conf.json`

```json
{
  "/api": {
    "target": "http://localhost:8081",
    "secure": false
  }
}
```

Wire it into `angular.json` under
`projects → frontend → architect → serve → options`:

```json
"options": {
  "proxyConfig": "proxy.conf.json"
}
```

### 5.4 Run

```bash
ng serve
```

Open <http://localhost:4200> — the default Angular page.

### 5.5 Confirm the test runner works

`ng new` generated a spec for the root component. Run it before writing any of
your own:

```bash
ng test --watch=false
```

Your CLI uses **Karma + Jasmine** (older) or **Vitest** (newer) — `ng test` tells
you which. The `describe` / `it` / `expect` / `TestBed` code in Phase 6 works on
both; only a couple of flags differ (noted there).

> **Verify Phase 5**
> `:4200` shows the starter page, no console errors. `ng test --watch=false`
> exits `0` with the generated spec passing. Stop `ng serve` with `Ctrl+C`.

---

## Phase 6 — The guestbook feature (~2 h with tests)

Everything here is generated with `ng generate` (`ng g`). Run the command, then
**read the file it made** — the class name and filename depend on your CLI
version. Import paths below assume `models/message.ts` and `services/message.ts`;
adjust to what you actually got.

### 6.1 Models — `ng generate interface models/message`

```bash
ng generate interface models/message
```

**Contract.** In the generated file, two interfaces:

- `Message` — `id: number; name: string; message: string; createdAt: string`
  (dates arrive as ISO strings over JSON).
- `CreateMessage` — `name: string; message: string; passcode: string`.

<details><summary>▸ Reference implementation</summary>

```typescript
export interface Message {
  id: number;
  name: string;
  message: string;
  createdAt: string;
}

export interface CreateMessage {
  name: string;
  message: string;
  passcode: string;
}
```
</details>

### 6.2 Service — `ng generate service services/message`

```bash
ng generate service services/message
```

This also creates `message.spec.ts` (used in 6.8). **Contract** for the service
(class is `MessageService` whatever the filename):

- `providedIn: 'root'`, `private http = inject(HttpClient)`, `baseUrl = '/api/messages'`.
- `list(): Observable<Message[]>` → `http.get`.
- `create(payload: CreateMessage): Observable<Message>` → `http.post`.

<details><summary>▸ Reference implementation</summary>

```typescript
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateMessage, Message } from '../models/message';

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/messages';

  list(): Observable<Message[]> {
    return this.http.get<Message[]>(this.baseUrl);
  }

  create(payload: CreateMessage): Observable<Message> {
    return this.http.post<Message>(this.baseUrl, payload);
  }
}
```
</details>

### 6.3 Component — `ng generate component guestbook`

```bash
ng generate component guestbook
```

Four files in `guestbook/` (`.ts`, `.html`, `.css`, `.spec.ts`). **Contract** for
the `.ts`:

- standalone, `imports: [ReactiveFormsModule]` (plus `DatePipe` if you use the
  `date` pipe in the template — or keep `CommonModule`).
- injected: `FormBuilder`, `MessageService`.
- signals: `messages = signal<Message[]>([])`, `submitting = signal(false)`,
  `error = signal<string | null>(null)`, `success = signal(false)`.
- `form` — `fb.nonNullable.group` with `name` (required, maxLength 100),
  `message` (required, maxLength 2000), `passcode` (required).
- `ngOnInit` → `reload()`.
- `reload()` → `service.list().subscribe(...)`, set `messages` / `error`.
- `submit()` → if `form.invalid` mark touched and bail; else set `submitting`,
  clear `error`/`success`, `service.create(form.getRawValue())`, on success reset
  the form and `reload()`, on error map status → message
  (`403` → "Wrong passcode.", `400` → "Please fill in name and message.",
  else → "Something went wrong. Try again.").

<details><summary>▸ Reference implementation</summary>

```typescript
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Message } from '../models/message';
import { MessageService } from '../services/message';

@Component({
  selector: 'app-guestbook',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './guestbook.html',
  styleUrl: './guestbook.css',
})
export class Guestbook implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(MessageService);

  messages = signal<Message[]>([]);
  submitting = signal(false);
  error = signal<string | null>(null);
  success = signal(false);

  form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    message: ['', [Validators.required, Validators.maxLength(2000)]],
    passcode: ['', Validators.required],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.service.list().subscribe({
      next: (msgs) => this.messages.set(msgs),
      error: () => this.error.set('Could not load messages.'),
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.success.set(false);

    this.service.create(this.form.getRawValue()).subscribe({
      next: () => {
        this.success.set(true);
        this.form.reset();
        this.submitting.set(false);
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        if (err.status === 403) this.error.set('Wrong passcode.');
        else if (err.status === 400) this.error.set('Please fill in name and message.');
        else this.error.set('Something went wrong. Try again.');
      },
    });
  }
}
```
</details>

### 6.4 Template — `guestbook.html`

A reactive form (`[formGroup]`, `(ngSubmit)`) with `name` / `message` / `passcode`
controls, a submit button disabled while `submitting()`, `@if` blocks for
`success()` / `error()`, and an `@for` list of `messages()` tracked by `m.id`.

<details><summary>▸ Reference implementation</summary>

```html
<section class="card">
  <h1>Sign the guestbook</h1>

  <form [formGroup]="form" (ngSubmit)="submit()">
    <label>Name <input type="text" formControlName="name" /></label>
    <label>Message <textarea rows="4" formControlName="message"></textarea></label>
    <label>Passcode <input type="password" formControlName="passcode" /></label>

    <button type="submit" [disabled]="submitting()">
      {{ submitting() ? 'Sending…' : 'Submit' }}
    </button>

    @if (success()) { <p class="ok">Thanks! Your message was added.</p> }
    @if (error()) { <p class="err">{{ error() }}</p> }
  </form>
</section>

<section class="card">
  <h2>Messages</h2>
  @if (messages().length === 0) {
    <p>No messages yet.</p>
  } @else {
    <ul class="messages">
      @for (m of messages(); track m.id) {
        <li>
          <strong>{{ m.name }}</strong>
          <span class="date">{{ m.createdAt | date: 'medium' }}</span>
          <p>{{ m.message }}</p>
        </li>
      }
    </ul>
  }
</section>
```
</details>

### 6.5 Styles — `guestbook.css`

Cosmetic only. Grab the reference or write your own.

<details><summary>▸ Reference implementation</summary>

```css
:host { display: block; max-width: 640px; margin: 2rem auto; font-family: system-ui, sans-serif; }
.card { border: 1px solid #ddd; border-radius: 8px; padding: 1.5rem; margin-bottom: 1.5rem; }
label { display: block; margin-bottom: 1rem; font-weight: 600; }
input, textarea { display: block; width: 100%; margin-top: 0.25rem; padding: 0.5rem; font: inherit; box-sizing: border-box; }
button { padding: 0.5rem 1rem; font: inherit; cursor: pointer; }
.ok { color: #157347; }
.err { color: #b02a37; }
.messages { list-style: none; padding: 0; }
.messages li { border-top: 1px solid #eee; padding: 0.75rem 0; }
.date { color: #888; font-size: 0.85rem; margin-left: 0.5rem; }
```
</details>

### 6.6 Mount it in the root component

Open the root component (`src/app/app.ts` or `src/app/app.component.ts`). Add the
guestbook component to `imports`, and replace the template with just
`<app-guestbook />` (delete the CLI's starter markup, or the linked `.html`
contents).

### 6.7 Run

```bash
ng serve
```

> **Verify Phase 6 (run)**
> `:4200` shows the form and an empty "Messages" section, no console errors.
> (Submitting fails until the backend is up — Phase 7.)

---

### Frontend unit tests

`ng generate` already made the spec files. Fill them in.

### 6.8 `services/message.spec.ts` — HTTP contract

**Contract.** Configure `TestBed` with `provideHttpClient()` +
`provideHttpClientTesting()`; inject the service and `HttpTestingController`;
`afterEach` → `httpMock.verify()`. Cases:

- `list()` → subscribe, `expectOne('/api/messages')`, assert method `GET`,
  `req.flush(fakeArray)`, assert the emitted value.
- `create(payload)` → subscribe, `expectOne`, assert method `POST` and
  `req.request.body`, `flush` a fake `Message`.

<details><summary>▸ Reference implementation</summary>

```typescript
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Message } from '../models/message';
import { MessageService } from './message';

describe('MessageService', () => {
  let service: MessageService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MessageService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list() GETs /api/messages', () => {
    const fake: Message[] = [
      { id: 1, name: 'Ada', message: 'hi', createdAt: '2026-01-01T00:00:00Z' },
    ];
    let result: Message[] | undefined;
    service.list().subscribe((r) => (result = r));

    const req = httpMock.expectOne('/api/messages');
    expect(req.request.method).toBe('GET');
    req.flush(fake);

    expect(result).toEqual(fake);
  });

  it('create() POSTs the payload', () => {
    const payload = { name: 'Ada', message: 'hi', passcode: 'let-me-in' };
    service.create(payload).subscribe();

    const req = httpMock.expectOne('/api/messages');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 1, ...payload, createdAt: '2026-01-01T00:00:00Z' });
  });
});
```
</details>

### 6.9 `guestbook/guestbook.spec.ts` — component behaviour

**Contract.** `TestBed` with the component in `imports` and the same HTTP-testing
providers. In `beforeEach`, `fixture.detectChanges()` triggers `ngOnInit` →
`reload()`, so flush the initial `GET` with `[]`. Cases:

- component is created.
- empty form → `submit()` sends no request (`httpMock.expectNone`), form is invalid.
- `403` on submit → `component.error()` is `"Wrong passcode."`.

<details><summary>▸ Reference implementation</summary>

```typescript
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Guestbook } from './guestbook';

describe('Guestbook', () => {
  let fixture: ComponentFixture<Guestbook>;
  let component: Guestbook;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Guestbook],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Guestbook);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    httpMock.expectOne('/api/messages').flush([]); // the ngOnInit reload()
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(component).toBeTruthy();
  });

  it('does not submit an empty form', () => {
    component.submit();
    httpMock.expectNone('/api/messages');
    expect(component.form.invalid).toBe(true);
  });

  it('shows "Wrong passcode." on a 403', () => {
    component.form.setValue({ name: 'Ada', message: 'hi', passcode: 'nope' });
    component.submit();

    httpMock.expectOne('/api/messages')
      .flush({ detail: 'Invalid passcode' }, { status: 403, statusText: 'Forbidden' });

    expect(component.error()).toBe('Wrong passcode.');
  });
});
```
</details>

### 6.10 Run the frontend suite

```bash
ng test --watch=false        # Karma; on a Vitest CLI just `ng test` runs once
```

> **Verify Phase 6 (test)**
> All specs green: the generated root-component spec, `MessageService` (2),
> `Guestbook` (3).

---

## Phase 7 — End-to-end in dev mode (~25 min)

Green tests first:

```bash
( cd backend && ./mvnw test )
( cd frontend && ng test --watch=false )
```

Then three terminals:

```bash
# 1 — database
cd ~/workspace/fullstack-practice-2 && docker compose up -d
# 2 — backend
cd ~/workspace/fullstack-practice-2/backend && ./mvnw spring-boot:run
# 3 — frontend
cd ~/workspace/fullstack-practice-2/frontend && ng serve
```

At <http://localhost:4200>:

1. Name + message + passcode `let-me-in` → green success, row appears below.
2. Passcode `nope` → red "Wrong passcode."
3. Empty name → blocked client-side.
4. Reload the page → messages persist (they're in Postgres).

> **Verify Phase 7**
> All four behaviours, then:
> ```bash
> docker compose exec db psql -U guestbook -d guestbook -c 'select name, body, created_at from messages order by created_at desc;'
> ```
> Your submissions are listed. Stop backend + frontend. **This is a complete
> working full-stack app.** Phases 8–10 package it.

---

## Phase 8 — Dockerize the backend (~45 min)

### 8.1 `backend/Dockerfile`

```dockerfile
# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> **`-DskipTests` in the image build is deliberate.** The backend suite needs a
> Docker daemon (Testcontainers), which isn't available inside the build. Run
> `./mvnw test` locally / in CI before building the image — the image trusts that
> green run.

### 8.2 `backend/.dockerignore`

```
target/
.git/
*.iml
```

### 8.3 Build and smoke-test against the dev DB

```bash
cd ~/workspace/fullstack-practice-2/backend
docker build -t guestbook-backend .

docker run --rm --name gb-backend \
  --network fullstack-practice-2_default \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e DB_USERNAME=guestbook -e DB_PASSWORD=guestbook \
  -e APP_SUBMISSION_PASSCODE=let-me-in \
  -p 8081:8081 \
  guestbook-backend
```

(Confirm the network name with `docker network ls` — it's `<folder>_default`.)

> **Verify Phase 8**
> ```bash
> curl -s localhost:8081/actuator/health      # {"status":"UP"}
> curl -s localhost:8081/api/messages          # your earlier rows
> ```
> `Ctrl+C` to stop.

---

## Phase 9 — Dockerize the frontend + nginx proxy (~45 min)

### 9.1 Confirm the build output path

```bash
cd ~/workspace/fullstack-practice-2/frontend
npm run build
ls dist/frontend            # expect a "browser" folder
```

If files land directly in `dist/frontend`, drop `/browser` from the `COPY` in 9.2.
`npm run build` does **not** run tests — CI runs `ng test --watch=false` as a
separate step (Appendix B).

### 9.2 `frontend/Dockerfile`

```dockerfile
# --- build stage ---
FROM node:22 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# --- serve stage ---
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
```

### 9.3 `frontend/nginx.conf`

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://backend:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### 9.4 `frontend/.dockerignore`

```
node_modules/
dist/
.angular/
```

> **Verify Phase 9**
> ```bash
> docker build -t guestbook-frontend ~/workspace/fullstack-practice-2/frontend
> ```
> Build completes. (It needs `backend` on the network to actually serve API calls
> — Phase 10.)

---

## Phase 10 — Full stack via Compose (~30 min)

### 10.1 `docker-compose.full.yml` (project root)

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: ${DB_NAME:-guestbook}
      POSTGRES_USER: ${DB_USERNAME:-guestbook}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-guestbook}
    volumes:
      - guestbook_pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 5s
      timeout: 5s
      retries: 10

  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: docker
      APP_SUBMISSION_PASSCODE: ${APP_SUBMISSION_PASSCODE:-let-me-in}
      DB_USERNAME: ${DB_USERNAME:-guestbook}
      DB_PASSWORD: ${DB_PASSWORD:-guestbook}
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8081/actuator/health | grep -q UP"]
      interval: 10s
      timeout: 5s
      retries: 12

  frontend:
    build: ./frontend
    ports:
      - "8080:80"
    depends_on:
      backend:
        condition: service_healthy

volumes:
  guestbook_pgdata:
```

### 10.2 Run from cold

```bash
cd ~/workspace/fullstack-practice-2
docker compose down                                  # stop the dev DB
docker compose -f docker-compose.full.yml up --build
```

Wait for `frontend` to start, open <http://localhost:8080>.

### 10.3 Exercise it

Same four checks as Phase 7 — requests now go Angular → nginx → Spring Boot → Postgres.

```bash
curl -s localhost:8080/api/messages
```

### 10.4 Shut down

```bash
docker compose -f docker-compose.full.yml down          # keep data
docker compose -f docker-compose.full.yml down -v       # wipe the DB volume
```

> **Verify Phase 10**
> A fresh `up --build` from a clean state gives a working guestbook at `:8080`
> with no manual steps. **Done.**

---

## Phase 11 — Write / update RUNBOOK.md (~30 min)

Keep `RUNBOOK.md` terse — it's for *you* on the next build. Each step from this
guide should have a `[ ]` line and a **Notes:** space. Suggested skeleton:

```markdown
## 2. Backend  (spring init: web,data-jpa,postgresql,validation,actuator,testcontainers)
- application.yml in src/main/resources/, ${VAR:default} syntax, spring.config.import ../.env
- Testcontainers: 3 deps, TestcontainersConfiguration, @Import on GuestbookApplicationTests
- ./mvnw test green

## 3. Domain + tests
- config/{AppProperties,WebConfig}  message/{Message,Repository,CreateMessageRequest,
  MessageResponse,InvalidPasscodeException,Service,Controller}  error/ApiExceptionHandler
- tests: MessageServiceTest (Mockito), MessageRepositoryTest (@DataJpaTest+TC),
  MessageControllerTest (@WebMvcTest+@MockitoBean)
- ./mvnw test

## 5-6. Frontend + tests  (ng new; ng g interface/service/component)
- provideHttpClient; proxy.conf.json
- models/message, services/message, guestbook component
- specs: message.spec (HttpTestingController), guestbook.spec (TestBed)
- ng test --watch=false

## 8-10. Dockerize  (backend -DskipTests; frontend nginx /api->backend:8081; full compose)
```

---

## Appendix A — Testing quick reference

### Backend

| Need | Annotation / tool | Notes |
|---|---|---|
| pure logic, no Spring | `@ExtendWith(MockitoExtension.class)`, `@Mock`, `ArgumentCaptor` | fastest; no context |
| repository / derived queries | `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import(TestcontainersConfiguration.class)` | rolls back each test |
| controller / JSON / validation / status | `@WebMvcTest(X.class)`, `@Autowired MockMvc`, `@MockitoBean` | loads MVC + `@RestControllerAdvice` only |
| full wiring | `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)` | needs Docker |
| replace a bean with a mock | `@MockitoBean` / `@MockitoSpyBean` | `@MockBean` was **removed** in Boot 4 |
| assertions | AssertJ `assertThat(...)` | bundled with the test starters |

### Frontend

| Need | Tool |
|---|---|
| test module | `TestBed.configureTestingModule({ imports, providers })` |
| fake HTTP | `provideHttpClient()` + `provideHttpClientTesting()`, then `HttpTestingController` |
| assert one request | `httpMock.expectOne(url)` → check `.request.method` / `.request.body` → `.flush(body)` / `.flush(body, { status, statusText })` |
| assert no request | `httpMock.expectNone(url)` |
| always | `afterEach(() => httpMock.verify())` |
| render a component | `TestBed.createComponent(X)` → `fixture.detectChanges()` (runs `ngOnInit`) |
| run once | `ng test --watch=false` (Karma) / `ng test` (Vitest) |

---

## Appendix B — Optional CI (GitHub Actions)

`.github/workflows/ci.yml` — the runners have Docker, so Testcontainers works:

```yaml
name: CI
on: [push, pull_request]
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21', cache: maven }
      - run: ./mvnw -B verify
        working-directory: backend
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22', cache: npm, cache-dependency-path: frontend/package-lock.json }
      - run: npm ci
        working-directory: frontend
      - run: npx ng test --watch=false --browsers=ChromeHeadless
        working-directory: frontend
      - run: npm run build
        working-directory: frontend
```

---

## The practice loop

| Build | How | Target time |
|-------|-----|-------------|
| 1 | Original guide, top to bottom | 1–2 days |
| 2 | This guide — CLI-first, tests included. RUNBOOK as you go. | ~1 day |
| 3 | RUNBOOK only; this guide as fallback. Fix RUNBOOK wherever you peeked. | ~4 h |
| 4+ | Add one deferred technology as a new phase, keep it in the rebuild. | — |

Always start from an empty directory.

---

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `mvn`/`ng`/`java` not found in a new shell | SDKMAN/nvm init lines are in `~/.bashrc`; open a new login shell or `source ~/.bashrc`. |
| Backend: `Failed to determine a suitable driver class` at startup | `application.yml` not on the classpath — wrong dir (`src/resources/` instead of `src/main/resources/`), or a competing `application.yaml`. Check `ls backend/target/classes`. |
| A property value literally contains `-guestbook` / `-let-me-in` | You used shell syntax `${VAR:-default}` in a Spring file. Spring uses `${VAR:default}` (single colon). |
| Backend: `role "…" does not exist` or `password authentication failed` after editing `.env` | Postgres volume predates the change; it only seeds the user on first run. `docker compose down -v && docker compose up -d`. |
| Backend: `APPLICATION FAILED TO START` — `required a bean of type '…AppProperties'` | `@ConfigurationPropertiesScan` missing from `GuestbookApplication` (3.1). |
| POM: `'dependencies.dependency.version' for org.testcontainers:postgresql:jar is missing` | Testcontainers 1.x artifact name under a Boot 4.1 BOM (Testcontainers 2.x). Use `testcontainers-postgresql` / `testcontainers-junit-jupiter`. |
| Tests: `Could not find a valid Docker environment` | Docker Desktop not running, or WSL integration disabled. |
| `@MockBean` won't resolve | Removed in Boot 4 — use `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`). |
| `ng test` can't launch Chrome | Install Chrome/Chromium and `export CHROME_BIN=$(which chromium)`, or use `--browsers=ChromeHeadless`, or the Vitest runner. |
| `ng g` produced `guestbook.component.ts` / class `GuestbookComponent` | Older CLI. Fine — just match your imports to the real names. |
| Angular calls 404 for `/api/...` | Proxy not active. `ng serve` (proxy wired in `angular.json`) or `ng serve --proxy-config proxy.conf.json`. |
| CORS error in the browser (dev) | `WebConfig` missing, or `app.cors-allowed-origins` doesn't match `http://localhost:4200` exactly. |
| `COPY --from=build /app/dist/frontend/browser` fails | Your Angular output path differs. `ls frontend/dist/frontend` and adjust. |
| nginx: `host not found in upstream "backend"` | Frontend image run without the Compose network. Use `docker-compose.full.yml`. |
| Port already in use (5432/8080/8081) | Leftover container. `docker ps`, stop it, or remap the host port. |

---

## What you can say you've built

- A REST API in **Java 21 / Spring Boot 4**, built with **Maven**, over
  **PostgreSQL** via **Spring Data JPA**, with DTO validation, a shared-secret
  gate on writes, and RFC-7807 problem responses.
- A layered **test suite**: Mockito unit tests, `@DataJpaTest` / `@WebMvcTest`
  slices, and a **Testcontainers** integration tier — the same suite a CI
  pipeline runs.
- An **Angular 20+** SPA — reactive form, `HttpClient` service, signal state,
  modern control-flow templates — with **`TestBed`** component tests and
  **`HttpTestingController`** service tests.
- A dev proxy for same-origin calls, and a production packaging where **nginx**
  serves the built SPA and reverse-proxies the API.
- The whole thing reproducible with **Docker Compose** — `up --build` from a
  clean checkout yields a working app.
