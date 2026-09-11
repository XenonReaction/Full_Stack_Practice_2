# RUNBOOK - second build progress tracker

Checklist going through the build for a second time.  Now that the project proof of concept exists, I want to implement pieces of the technology I didn't use last time, while re-enforcing the tools I used the first time.

Legend: `[ ]` todo · `[x]` done · `[~]` done with a caveat (see notes)

---

## Phase 0 — Prerequisites

### 0.0 Base Packages
- [x] Check all of the tools are working and present.

**Notes:**
>   Ran "sudo apt upgrade && sudo apt upgrade && sudo apt install -y curl zip unzip". Made sure I was on WSL, and we have zip and unzip installed.

### 0.1 Docker Desktop
- [x] Docker Desktop on
- [x] WSL integration for docker setup
- [x] Running 'hello-world'
- [x] Check version

**Notes:**
>   Works!

### 0.2 Java 21 + maven via SDKMAN
- [x] a. `curl -s "https://get.sdkman.io" | bash`
- [x] b. `source "$HOME/.sdkman/bin/sdkman-init.sh"`
- [x] c. `sdk list java | grep -i tem` (find current 21.x Temurin id)
- [x] d. `sdk install java 21.0.x-tem`
- [x] e. `sdk install maven`
- [x] f. `java -version` → 21.x
- [x] g. `mvn -version` → Maven 3.9.x on Java 21
- [x] h. open a fresh shell, re-run f & g
- [x] i. `which java && which mvn` → both under `~/.sdkman/...` (not `/mnt/c/...` or `/usr/lib/jvm`)

**Notes:**
>   Works

### 0.3 Node 22 + Angular CLI via nvm
- [x] a. `curl -o- .../nvm/v0.40.1/install.sh | bash`
- [x] b. close and reopen the shell
- [x] c. `nvm install 22`
- [x] d. `nvm alias default 22`
- [x] e. `node -v` → v22.x
- [x] f. `npm -v`
- [x] g. `npm install -g @angular/cli`
- [x] h. `ng version`

**Notes:**
>   These are already installed. ran "node -v && npm -v && ng version"

### ✅ Verify Phase 0
- [x] `docker run --rm hello-world && java -version && mvn -version && node -v && ng version` — all run

**Notes:**
>   There were other steps, but they were setup stuff I skipped. Got this command to return the versions.

---

## Phase 1 — Project skeleton + PostgreSQL

### 1.1 Folders
- [x] `mkdir -p ~/workspace/fullstack-practice` then `mkdir backend frontend`

**Notes:**
>   check

### 1.2 `docker-compose.yml` (Postgres only)
- [x] created with `postgres:16`, db/user/pass = `guestbook`, port 5432, volume, healthcheck

**Notes:**
>   This was a copy and paste step last time.  I want to see how to do it for myself.
>   Looked it up.  You have to read a bunch of documentation and write the file yourself.  Don't want to spend the time to do that right now.

### 1.3 `.env`
- [x] `.env.example` created (`APP_SUBMISSION_PASSCODE=let-me-in`)
- [x] `cp .env.example .env`

**Notes:**
>   Set this up, but just the .env file

### 1.4 `.gitignore`
- [x] created

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

### 2.1 Generate
- [x] `curl https://start.spring.io/starter.zip ...` (web, data-jpa, postgresql, validation, actuator)
- [x] unzip into `backend/` and flatten the inner folder
- [x] `ls backend` shows `pom.xml`, `src`, `mvnw`

**Notes:**
>   Didn't use the long curl command this time.  Went to the spring initializer and got the .zip myself and placed it in the backend directory.

### 2.2 `application.yml`
- [x] delete `application.properties`, create `application.yml` (port 8081, datasource localhost, ddl-auto update, `app.submission-passcode`)

**Notes:**
>   The last time, the db username and password were hardcoded into a lot of the files.  I set them up as .env variables.

### 2.3 `application-docker.yml`
- [x] created (datasource host = `db`)

**Notes:**
>   don't think i need this. we will see later.

### 2.4 First run
- [x] `cd backend && ./mvnw spring-boot:run` → `Started GuestbookApplication`

**Notes:**
>   I made 'application.yml' in the wrong spot and gave it the wrong name. should have been in src/main/resources. also made 'application-docker.yml', and it worked?  there was also a problem where the enviornment variables were not referenced correctly.  one file needed a '-' in front of the back up option and the other didn't.

### ✅ Verify Phase 2
- [x] `curl -s localhost:8081/actuator/health` → `{"status":"UP"}`

**Notes:**
>   got it working.

---

## Phase 3 — Backend domain

Create each file (full contents in guide §3):
- [x] 3.1 `GuestbookApplication.java` — add `@ConfigurationPropertiesScan`

**Notes:**
>   

- [x] 3.2 `config/AppProperties.java`

**Notes:**
>   

- [x] 3.3 `config/WebConfig.java` (dev CORS)

**Notes:**
>   

- [x] 3.4 `message/Message.java` (`@Entity`)

**Notes:**
>   

- [x] 3.5 `message/MessageRepository.java`

**Notes:**
>   

- [x] 3.6 `message/CreateMessageRequest.java` (validated DTO in)

**Notes:**
>   

- [x] 3.7 `message/MessageResponse.java` (DTO out — no passcode)

**Notes:**
>   This file helps convert the response from the database into a format easily readable and usable in Java

- [x] 3.8 `message/InvalidPasscodeException.java`

**Notes:**
>   

- [x] 3.9 `message/MessageService.java` (passcode check + save)

**Notes:**
>   

- [x] 3.10 `message/MessageController.java` (GET + POST)

**Notes:**
>   

- [x] 3.11 `error/ApiExceptionHandler.java`

**Notes:**
>   

- [x] 3.12 `./mvnw spring-boot:run` — log shows `create table messages ...`

**Notes:**
>   

### ✅ Verify Phase 3
- [x] `docker compose exec db psql -U guestbook -d guestbook -c '\d messages'` → columns `id, name, body, created_at`

**Notes:**
>   

---

# Additions for build 2 — tests + CLI-first (appended 2026-09-10)

Everything above stays as-is. The steps below track the parts of the **updated
BUILD_GUIDE.md** that weren't in the first pass: the `.env` / Testcontainers fixes
in Phase 2, unit tests in Phases 3 and 6, and Phases 4–11.

Legend unchanged: `[ ]` todo · `[x]` done · `[~]` done with a caveat.

---

## Phase 2 (additions) — `.env` loading + Testcontainers

### 2.2b `application.yml` — right place, right syntax
- [ ] file is at `backend/src/main/resources/application.yml` (not `src/resources/`, no competing `application.yaml`)
- [ ] `ls backend/target/classes` after a build shows `application.yml`
- [ ] defaults use Spring syntax `${VAR:default}` (single colon), not `${VAR:-default}`
- [ ] `spring.config.import: optional:file:../.env[.properties]` present

**Notes:**
>   

### 2.5 `.env` reaches Spring without exporting
- [ ] `./mvnw spring-boot:run` from `backend/` picks up `APP_SUBMISSION_PASSCODE` / `DB_*` from repo-root `.env`
- [ ] changing the passcode in `.env` changes the accepted passcode on next run

**Notes:**
>   

### 2.6 Testcontainers baseline
- [ ] added `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql` (test scope) to `pom.xml`
- [ ] `src/test/java/.../TestcontainersConfiguration.java` — `@TestConfiguration` + `@Bean @ServiceConnection PostgreSQLContainer<?>`
- [ ] `GuestbookApplicationTests` annotated `@Import(TestcontainersConfiguration.class)`
- [ ] (optional) `TestGuestbookApplication` main for `./mvnw spring-boot:test-run`

**Notes:**
>   

### ✅ Verify Phase 2 (test)
- [ ] Docker running; `./mvnw test` → `BUILD SUCCESS`, `Tests run: 1`
- [ ] `docker ps` during the run shows a transient `postgres:16` container

**Notes:**
>   

---

## Phase 3 (additions) — backend unit tests

All under `backend/src/test/java/com/example/guestbook/`.

### 3.13 `message/MessageServiceTest.java` — pure Mockito
- [ ] `@ExtendWith(MockitoExtension.class)`, `@Mock MessageRepository`, real `AppProperties`
- [ ] wrong passcode → throws `InvalidPasscodeException`, `verify(repository, never()).save(any())`
- [ ] good passcode → `ArgumentCaptor` shows `name` / `body` trimmed
- [ ] `findAll()` → maps entities to `MessageResponse`

**Notes:**
>   

### 3.14 `message/MessageRepositoryTest.java` — `@DataJpaTest` slice
- [ ] `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import(TestcontainersConfiguration.class)`
- [ ] `findAllByOrderByCreatedAtDesc()` returns newest-first

**Notes:**
>   

### 3.15 `message/MessageControllerTest.java` — `@WebMvcTest` slice
- [ ] `@WebMvcTest(MessageController.class)`, `@Autowired MockMvc`, `@MockitoBean MessageService`
- [ ] `GET` → 200, `$[0].name`, `$[0].passcode` does not exist
- [ ] `POST` blank name → 400
- [ ] `POST` + service throws `InvalidPasscodeException` → 403

**Notes:**
>   

### 3.16 Run the suite
- [ ] `./mvnw test` — `MessageServiceTest`, `MessageRepositoryTest`, `MessageControllerTest`, `GuestbookApplicationTests` all green

**Notes:**
>   

---

## Phase 4 — Manual API check with curl

With `docker compose up -d` + `./mvnw spring-boot:run`:
- [ ] valid submission → `201`, body has `id` + `createdAt`, no `passcode`
- [ ] wrong passcode → `403`
- [ ] blank fields → `400`
- [ ] `GET /api/messages` → `200`, newest first

### ✅ Verify Phase 4
- [ ] `select id, name, body from messages;` → one row. Backend stopped.

**Notes:**
>   

---

## Phase 5 — Angular frontend scaffold

### 5.1 Generate
- [ ] `ng new frontend --style=css --ssr=false`, `cd frontend`
- [ ] noted the CLI major version + whether the runner is Karma or Vitest

**Notes:**
>   

### 5.2 HttpClient
- [ ] `provideHttpClient()` added to `providers` in `src/app/app.config.ts`

**Notes:**
>   

### 5.3 Dev proxy
- [ ] `frontend/proxy.conf.json` → `/api` to `http://localhost:8081`
- [ ] `angular.json` serve.options.proxyConfig set

**Notes:**
>   

### 5.4 Run
- [ ] `ng serve` → starter page at `:4200`

**Notes:**
>   

### 5.5 Test runner works
- [ ] `ng test --watch=false` → generated root-component spec passes, exit 0

**Notes:**
>   

### ✅ Verify Phase 5
- [ ] `:4200` loads, no console errors; `ng test --watch=false` green

**Notes:**
>   

---

## Phase 6 — The guestbook feature

Adjust filenames/class names to whatever `ng generate` actually printed.

### 6.1 Models
- [ ] `ng generate interface models/message`
- [ ] `Message` + `CreateMessage` interfaces filled in

**Notes:**
>   

### 6.2 Service
- [ ] `ng generate service services/message`
- [ ] `list()` / `create()` implemented against `/api/messages`

**Notes:**
>   

### 6.3 Component (TS)
- [ ] `ng generate component guestbook`
- [ ] signals (`messages`, `submitting`, `error`, `success`), reactive `form`, `ngOnInit → reload()`, `submit()` with 403/400/other mapping

**Notes:**
>   

### 6.4 Template
- [ ] `[formGroup]` form, disabled-while-submitting button, `@if` success/error, `@for` message list tracked by `m.id`

**Notes:**
>   

### 6.5 Styles
- [ ] `guestbook.css` (reference or own)

**Notes:**
>   

### 6.6 Mount
- [ ] guestbook component added to root `imports`, root template = `<app-guestbook />`

**Notes:**
>   

### 6.7 Run
- [ ] `ng serve` → form + empty "Messages", no console errors

**Notes:**
>   

### 6.8 Service spec
- [ ] `provideHttpClient()` + `provideHttpClientTesting()`, `HttpTestingController`, `afterEach verify()`
- [ ] `list()` GET asserted; `create()` POST body asserted

**Notes:**
>   

### 6.9 Component spec
- [ ] `TestBed` with component + HTTP testing providers; flush the `ngOnInit` GET with `[]`
- [ ] created; empty form → no request + invalid; 403 → `error() === 'Wrong passcode.'`

**Notes:**
>   

### 6.10 Run the suite
- [ ] `ng test --watch=false` — root spec + `MessageService` (2) + `Guestbook` (3) green

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
- [ ] submit valid → green + row appears
- [ ] passcode `nope` → red "Wrong passcode."
- [ ] empty name → blocked client-side
- [ ] reload page → messages persist

### ✅ Verify Phase 7
- [ ] all four behaviours; `select ... from messages order by created_at desc;` lists submissions

**Notes:**
>   

---

## Phase 8 — Dockerize the backend

- [ ] `backend/Dockerfile` (maven build `-DskipTests` → jre-alpine runtime)
- [ ] `backend/.dockerignore`
- [ ] `docker build -t guestbook-backend .`
- [ ] `docker run` on `fullstack-practice-2_default` network, `SPRING_PROFILES_ACTIVE=docker`, `DB_USERNAME`/`DB_PASSWORD`/`APP_SUBMISSION_PASSCODE`

### ✅ Verify Phase 8
- [ ] `curl localhost:8081/actuator/health` → UP
- [ ] `curl localhost:8081/api/messages` → earlier rows

**Notes:**
>   

---

## Phase 9 — Dockerize the frontend + nginx

- [ ] `npm run build`; confirmed output path (`dist/frontend/browser`?)
- [ ] `frontend/Dockerfile` (node build → nginx)
- [ ] `frontend/nginx.conf` — `/api/` → `backend:8081`, SPA fallback `try_files`
- [ ] `frontend/.dockerignore`

### ✅ Verify Phase 9
- [ ] `docker build -t guestbook-frontend frontend` completes

**Notes:**
>   

---

## Phase 10 — Full stack via Compose

- [ ] `docker-compose.full.yml` (db + backend + frontend, healthchecks, `8080:80`)
- [ ] `docker compose down` (stop dev DB), then `docker compose -f docker-compose.full.yml up --build`
- [ ] four checks from Phase 7 at `:8080`
- [ ] `curl localhost:8080/api/messages`
- [ ] shutdown: `down` (keep data) vs `down -v` (wipe)

### ✅ Verify Phase 10
- [ ] fresh `up --build` from clean state → working guestbook at `:8080`, no manual steps

**Notes:**
>   

---

## Phase 11 — Update RUNBOOK.md

- [ ] every BUILD_GUIDE step has a `[ ]` line + Notes space here
- [ ] noted every place I opened a `▸ Reference implementation` instead of writing it myself

**Notes:**
>   

---

## (optional) Appendix B — CI

- [ ] `.github/workflows/ci.yml` — backend `./mvnw -B verify`, frontend `npm ci` + `ng test --watch=false --browsers=ChromeHeadless` + `npm run build`

**Notes:**
>   