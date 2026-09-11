# RUNBOOK - second build progress tracker

Checklist going through the build for a second time.  Now that the project proof of concept exists, I want to implement pieces of the technology I didn't use last time, while re-enforcing the tools I used the first time.

Reorganized to follow the updated `BUILD_GUIDE.md` (CLI-first, unit tests, Testcontainers). Notes from the earlier pass are kept in place; new steps are unchecked.

Legend: `[ ]` todo · `[x]` done · `[~]` done with a caveat (see notes)

---

## Phase 0 — Prerequisites

### 0.0 Base packages
- [x] `sudo apt update && sudo apt install -y curl zip unzip`

**Notes:**
>   Ran "sudo apt upgrade && sudo apt upgrade && sudo apt install -y curl zip unzip". Made sure I was on WSL, and we have zip and unzip installed.

### 0.1 Docker Desktop
- [x] Docker Desktop installed (Windows side)
- [x] WSL integration enabled for Ubuntu
- [x] `docker run --rm hello-world` succeeds
- [x] `docker compose version` succeeds

**Notes:**
>   Works!

### 0.2 Java 21 + Maven via SDKMAN
- [x] install SDKMAN, `source "$HOME/.sdkman/bin/sdkman-init.sh"`
- [x] `sdk list java | grep -i tem`, `sdk install java 21.0.x-tem`
- [x] `sdk install maven`
- [x] `java -version` → 21.x, `mvn -version` → Maven 3.9.x on Java 21
- [x] fresh shell: both still resolve, under `~/.sdkman/...`
- [x] (optional, new) `sdk install springboot`; `spring --version`; `spring init --list`

**Notes:**
>   Works

### 0.3 Node 22 + Angular CLI via nvm
- [x] install nvm, reopen shell
- [x] `nvm install 22`, `nvm alias default 22`
- [x] `node -v` → v22.x, `npm -v`
- [x] `npm install -g @angular/cli`
- [x] `ng version` — record the CLI major version and whether the test runner is Karma or Vitest

**Notes:**
>   These are already installed. ran "node -v && npm -v && ng version"

### 0.4 Editor
- [x] `code .` opens VS Code with the WSL extension attached
- [x] "Extension Pack for Java" + "Angular Language Service" installed

**Notes:**
>   got it

### ✅ Verify Phase 0
- [x] `docker run --rm hello-world && java -version && mvn -version && node -v && ng version` — all run

**Notes:**
>   There were other steps, but they were setup stuff I skipped. Got this command to return the versions.

---

## Phase 1 — Project skeleton + PostgreSQL

### 1.1 Folders
- [x] `mkdir -p ~/workspace/fullstack-practice-2`, then `mkdir backend frontend`

**Notes:**
>   check

### 1.2 `docker-compose.yml` (dev — Postgres only)
- [x] `postgres:16`, port 5432, named volume, healthcheck
- [x] credentials come from `.env` (`${DB_NAME:-guestbook}` / `${DB_USERNAME:-guestbook}` / `${DB_PASSWORD:-guestbook}`)
- [x] `$${...}` (double dollar) used for the vars inside the healthcheck command

**Notes:**
>   This was a copy and paste step last time.  I want to see how to do it for myself.
>   Looked it up.  You have to read a bunch of documentation and write the file yourself.  Don't want to spend the time to do that right now.

### 1.3 `.env.example` and `.env`
- [~] `.env.example` created (`APP_SUBMISSION_PASSCODE`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`)
- [x] `cp .env.example .env`

**Notes:**
>   Set this up, but just the .env file

### 1.4 `.gitignore`
- [x] created (`backend/target/`, `frontend/{node_modules,dist,.angular}/`, `.env`, ide files)

**Notes:**
>   Created and added .env to it

### 1.5 Start the database
- [x] `docker compose up -d`
- [x] `docker compose ps` shows `db` healthy

**Notes:**
>   works

### ✅ Verify Phase 1
- [x] `docker compose exec db psql -U guestbook -d guestbook -c '\dt'` → "Did not find any relations."

**Notes:**
>   works

---

## Phase 2 — Spring Boot backend scaffold

### 2.1 Generate the project
- [x] `spring init` / start.spring.io: Spring Web, Spring Data JPA, PostgreSQL Driver, Validation, Actuator, **Testcontainers**
- [x] unzip into `backend/`, flatten the inner folder; `ls backend` → `pom.xml`, `src`, `mvnw`
- [ ] confirm the three Testcontainers deps are actually in `pom.xml` (add them in 2.5 if not)

**Notes:**
>   Didn't use the long curl command this time.  Went to the spring initializer and got the .zip myself and placed it in the backend directory.

### 2.2 `backend/src/main/resources/application.yml`
- [x] file is at `src/main/resources/application.yml` — NOT `src/resources/`, and no competing `application.yaml`
- [x] `ls backend/target/classes` after a build shows `application.yml`
- [x] defaults use Spring syntax `${VAR:default}` (single colon), not the shell form `${VAR:-default}`
- [x] `spring.config.import: optional:file:../.env[.properties]` present
- [x] port 8081, datasource, ddl-auto update, `app.submission-passcode`, `app.cors-allowed-origins`

**Notes:**
>   The last time, the db username and password were hardcoded into a lot of the files.  I set them up as .env variables.
>   Double checked the extra steps after reconfiguring 'RUNBOOK.md' and 'BUILD_GUIDE.md'

### 2.3 `backend/src/main/resources/application-docker.yml`
- [x] datasource host = `db`, `${DB_USERNAME:guestbook}` / `${DB_PASSWORD:guestbook}`

**Notes:**
>   don't think i need this. we will see later.

### 2.4 First run
- [x] `cd backend && ./mvnw spring-boot:run` → `Started GuestbookApplication`

**Notes:**
>   I made 'application.yml' in the wrong spot and gave it the wrong name. should have been in src/main/resources. also made 'application-docker.yml', and it worked?  there was also a problem where the enviornment variables were not referenced correctly.  one file needed a '-' in front of the back up option and the other didn't.

### 2.5 Make the context test pass with Testcontainers
- [x] add `spring-boot-testcontainers`, `org.testcontainers:testcontainers-junit-jupiter`, `org.testcontainers:testcontainers-postgresql` (all `test` scope) — 2.x names, no version (BOM = 2.0.5)
- [x] `src/test/java/.../TestcontainersConfiguration.java` — `@TestConfiguration` + `@Bean @ServiceConnection PostgreSQLContainer` (`org.testcontainers.postgresql`, no `<>`)
- [x] `GuestbookApplicationTests` annotated `@Import(TestcontainersConfiguration.class)`
- [x] (optional) `TestGuestbookApplication` main for `./mvnw spring-boot:test-run`

**Notes:**
>   Had some problems with depricated code in the build guide. Adding dependencies to 'pom.xml' later works, but it felt stressful.  Would like to do it next time with the Spring Intializer.

### ✅ Verify Phase 2 (run)
- [x] `curl -s localhost:8081/actuator/health` → `{"status":"UP"}`

**Notes:**
>   got it working.

### ✅ Verify Phase 2 (test)
- [x] Docker running; `./mvnw test` → `BUILD SUCCESS`, `Tests run: 1`
- [x] `docker ps` during the run shows a transient `postgres:16` container

**Notes:**
>   tests run and dependencies are there.

---

## Phase 3 — Backend domain

Create each file — contract in guide §3, full source under **▸ Reference implementation**. Note here anywhere you opened the reference instead of writing it yourself.

- [x] 3.1 `GuestbookApplication.java` — add `@ConfigurationPropertiesScan`

**Notes:**
>   This is the annotation that allows Spring to look for properties to create the necessary beans.

- [x] 3.2 `config/AppProperties.java` — `@ConfigurationProperties(prefix = "app")`, `submissionPasscode` + `corsAllowedOrigins`

**Notes:**
>   Allows for password management

- [x] 3.3 `config/WebConfig.java` — dev CORS, origins read from `AppProperties`

**Notes:**
>   Keeps the CORS concerns under control

- [x] 3.4 `message/Message.java` — `@Entity`, getters only, `protected` no-arg ctor

**Notes:**
>   Structure for the message objects for Spring to create.  I don't know where the SQL database parameters exist.  I don't think we created a SQL database with statements, so I'm not sure what step that happens at.

- [x] 3.5 `message/MessageRepository.java` — derived query `findAllByOrderByCreatedAtDesc()`

**Notes:**
>   The interface is the contract for Spring to follow when creating the repo objects for messages.

- [x] 3.6 `message/CreateMessageRequest.java` — validated record (inbound)

**Notes:**
>   DTO (data transfer object), that's why it's 'record' instead of 'class'.

- [x] 3.7 `message/MessageResponse.java` — record (outbound), no passcode, `from(Message)`

**Notes:**
>   When a message is needed to be pulled from the database, this is the response that will return the message.

- [x] 3.8 `message/InvalidPasscodeException.java`

**Notes:**
>   simple exception structure

- [x] 3.9 `message/MessageService.java` — passcode check + trim + save

**Notes:**
>   done

- [x] 3.10 `message/MessageController.java` — GET + POST (201)

**Notes:**
>   done

- [x] 3.11 `error/ApiExceptionHandler.java` — 403 + 400 `ProblemDetail`

**Notes:**
>   done

- [x] 3.12 `./mvnw spring-boot:run` — log shows `create table messages ...`

**Notes:**
>   done

### ✅ Verify Phase 3 (run)
- [x] `docker compose exec db psql -U guestbook -d guestbook -c '\d messages'` → columns `id, name, body, created_at`

**Notes:**
>   done

### Backend unit tests  (under `backend/src/test/java/com/example/guestbook/`)

- [x] 3.13 `message/MessageServiceTest.java` — pure Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock` repo, real `AppProperties`)
  - [x] wrong passcode → throws `InvalidPasscodeException`, `verify(repository, never()).save(any())`
  - [x] good passcode → `ArgumentCaptor` shows `name` / `body` trimmed
  - [x] `findAll()` maps entities → `MessageResponse`

**Notes:**
> done

- [x] 3.14 `message/MessageRepositoryTest.java` — `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import(TestcontainersConfiguration.class)`; newest-first

**Notes:**
> Accidently had made 'TestcontainersConfiguration.java' private, so the imports weren't working. Change to public. There were other import problems that I have no idea how they got fixed; Claude did it.

- [x] 3.15 `message/MessageControllerTest.java` — `@WebMvcTest(MessageController.class)`, `@MockitoBean MessageService`
  - [x] GET → 200, `$[0].name`, `$[0].passcode` does not exist
  - [x] POST blank name → 400
  - [x] POST + service throws `InvalidPasscodeException` → 403

**Notes:**
> done

- [x] 3.16 `./mvnw test` — `MessageServiceTest`, `MessageRepositoryTest`, `MessageControllerTest`, `GuestbookApplicationTests` all green

**Notes:**
> They run. This was backwards, because we wrote the tests afterwards, but we can fix that next run.

### ✅ Verify Phase 3 (test)
- [x] `BUILD SUCCESS`; failures point at my code, not the test

**Notes:**
> runs and passes unit tests

---

## Phase 4 — Manual API check with curl

With `docker compose up -d` + `./mvnw spring-boot:run`:
- [x] valid submission → `201`, body has `id` + `createdAt`, no `passcode`
- [x] wrong passcode → `403`
- [x] blank fields → `400`
- [x] `GET /api/messages` → `200`, newest first

### ✅ Verify Phase 4
- [x] `select id, name, body from messages;` → one row; backend stopped

**Notes:**
> Allows us to test the actual function of the backend outside of unit tests.  Works.

---

## Phase 5 — Angular frontend scaffold

### 5.1 Generate
- [ ] `ng new frontend --style=css --ssr=false`; `cd frontend`
- [ ] recorded the CLI major version + runner (Karma / Vitest)

**Notes:**
>

### 5.2 Register HttpClient
- [ ] `provideHttpClient()` added to `providers` in `src/app/app.config.ts`

**Notes:**
>

### 5.3 Dev proxy
- [ ] `frontend/proxy.conf.json` → `/api` to `http://localhost:8081`
- [ ] `angular.json` → serve.options.proxyConfig set

**Notes:**
>

### 5.4 Run
- [ ] `ng serve` → starter page at `:4200`

**Notes:**
>

### 5.5 Confirm the test runner works
- [ ] `ng test --watch=false` → generated root-component spec passes, exit 0

**Notes:**
>

### ✅ Verify Phase 5
- [ ] `:4200` loads, no console errors; `ng test --watch=false` green

**Notes:**
>

---

## Phase 6 — The guestbook feature

Adjust filenames / class names to whatever `ng generate` actually printed.

### 6.1 Models — `ng generate interface models/message`
- [ ] `Message` + `CreateMessage` interfaces filled in

**Notes:**
>

### 6.2 Service — `ng generate service services/message`
- [ ] `list()` / `create()` implemented against `/api/messages`

**Notes:**
>

### 6.3 Component (TS) — `ng generate component guestbook`
- [ ] signals (`messages` / `submitting` / `error` / `success`), reactive `form`, `ngOnInit → reload()`, `submit()` with 403 / 400 / other mapping

**Notes:**
>

### 6.4 Template — `guestbook.html`
- [ ] `[formGroup]` form, button disabled while `submitting()`, `@if` success/error, `@for` list tracked by `m.id`

**Notes:**
>

### 6.5 Styles — `guestbook.css`
- [ ] reference or own

**Notes:**
>

### 6.6 Mount in the root component
- [ ] guestbook added to root `imports`, root template = `<app-guestbook />`

**Notes:**
>

### 6.7 Run
- [ ] `ng serve` → form + empty "Messages", no console errors

**Notes:**
>

### Frontend unit tests

- [ ] 6.8 `services/message.spec.ts` — `provideHttpClient()` + `provideHttpClientTesting()`, `HttpTestingController`, `afterEach verify()`; GET asserted, POST body asserted

**Notes:**
>

- [ ] 6.9 `guestbook/guestbook.spec.ts` — `TestBed`, flush the `ngOnInit` GET with `[]`; created / empty-form-sends-nothing / 403 → `error() === 'Wrong passcode.'`

**Notes:**
>

- [ ] 6.10 `ng test --watch=false` — root spec + `MessageService` (2) + `Guestbook` (3) green

**Notes:**
>

### ✅ Verify Phase 6
- [ ] feature renders at `:4200`; full frontend suite green

**Notes:**
>

---

## Phase 7 — End-to-end in dev mode

- [ ] `cd backend && ./mvnw test` green
- [ ] `cd frontend && ng test --watch=false` green
- [ ] 3 terminals: db / backend / frontend
- [ ] submit valid → green success + row appears
- [ ] passcode `nope` → red "Wrong passcode."
- [ ] empty name → blocked client-side
- [ ] reload page → messages persist (in Postgres)

### ✅ Verify Phase 7
- [ ] all four behaviours; `select ... from messages order by created_at desc;` lists submissions

**Notes:**
>

---

## Phase 8 — Dockerize the backend

- [ ] 8.1 `backend/Dockerfile` — maven build (`-DskipTests`) → `jre-alpine` runtime
- [ ] 8.2 `backend/.dockerignore`
- [ ] 8.3 `docker build -t guestbook-backend .`; `docker run` on `fullstack-practice-2_default` network, `SPRING_PROFILES_ACTIVE=docker`, `DB_USERNAME` / `DB_PASSWORD` / `APP_SUBMISSION_PASSCODE`

### ✅ Verify Phase 8
- [ ] `curl localhost:8081/actuator/health` → UP
- [ ] `curl localhost:8081/api/messages` → earlier rows

**Notes:**
>

---

## Phase 9 — Dockerize the frontend + nginx

- [ ] 9.1 `npm run build`; confirmed output path (`dist/frontend/browser`?)
- [ ] 9.2 `frontend/Dockerfile` — node build → nginx
- [ ] 9.3 `frontend/nginx.conf` — `/api/` → `backend:8081`, SPA fallback `try_files`
- [ ] 9.4 `frontend/.dockerignore`

### ✅ Verify Phase 9
- [ ] `docker build -t guestbook-frontend frontend` completes

**Notes:**
>

---

## Phase 10 — Full stack via Compose

- [ ] 10.1 `docker-compose.full.yml` — db + backend + frontend, healthchecks, `8080:80`
- [ ] 10.2 `docker compose down` (stop dev DB), then `docker compose -f docker-compose.full.yml up --build`
- [ ] 10.3 four checks from Phase 7 at `:8080`; `curl localhost:8080/api/messages`
- [ ] 10.4 shutdown: `down` (keep data) vs `down -v` (wipe volume)

### ✅ Verify Phase 10
- [ ] fresh `up --build` from a clean state → working guestbook at `:8080`, no manual steps

**Notes:**
>

---

## Phase 11 — Update this RUNBOOK

- [ ] every `BUILD_GUIDE.md` step has a `[ ]` line + Notes space here
- [ ] noted every place I opened a **▸ Reference implementation** instead of writing it myself

**Notes:**
>

---

## Appendix B — Optional CI (GitHub Actions)

- [ ] `.github/workflows/ci.yml` — backend `./mvnw -B verify`; frontend `npm ci` + `ng test --watch=false --browsers=ChromeHeadless` + `npm run build`

**Notes:**
>
