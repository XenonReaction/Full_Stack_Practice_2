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

