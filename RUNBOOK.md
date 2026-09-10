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
>   

### 2.2 `application.yml`
- [x] delete `application.properties`, create `application.yml` (port 8081, datasource localhost, ddl-auto update, `app.submission-passcode`)

**Notes:**
>   

### 2.3 `application-docker.yml`
- [x] created (datasource host = `db`)

**Notes:**
>   

### 2.4 First run
- [x] `cd backend && ./mvnw spring-boot:run` → `Started GuestbookApplication`

**Notes:**
>   

### ✅ Verify Phase 2
- [x] `curl -s localhost:8081/actuator/health` → `{"status":"UP"}`

**Notes:**
>   

---