# Unleash Maven Plugin — Integration & Debugging How-To

> **Task:** Plugin Integration & Debugging Workflow  
> **Area:** DevOps  
> **Author:** tsjahangiri  
> **Project:** device-management-service  
> **Integration Repository:** [tsjahangiri/device-management-service](https://github.com/tsjahangiri/device-management-service)  

---

## Table of Contents

1. [Overview](#1-overview)
2. [Prerequisites](#2-prerequisites)
3. [Plugin Integration into pom.xml](#3-plugin-integration-into-pomxml)
4. [One-Time Setup — Git & Credentials](#4-one-time-setup--git--credentials)
5. [Full Release Cycle Walkthrough](#5-full-release-cycle-walkthrough)
6. [Pitfalls & Fixes Encountered](#6-pitfalls--fixes-encountered)
7. [Debugging How-To — Attaching IntelliJ Debugger](#7-debugging-how-to--attaching-intellij-debugger)
8. [Available Goals Reference](#8-available-goals-reference)
9. [Lessons Learned & Recommendations](#9-lessons-learned--recommendations)

---

## 1. Overview

> 📦 **Reference Implementation:** This entire guide is based on hands-on integration performed in the following repository:  
> **[https://github.com/tsjahangiri/device-management-service](https://github.com/tsjahangiri/device-management-service)**  
> All configuration examples, pitfalls, and commands in this document reflect real issues encountered and resolved in that project.

### What Problem Does It Solve?

Without the plugin, every release requires a developer to manually:

1. Edit `pom.xml` — change `1.0.0-SNAPSHOT` → `1.0.0`
2. Run `mvn build`
3. Run `mvn deploy`
4. Run `git tag v1.0.0`
5. Run `git push`
6. Edit `pom.xml` again — change `1.0.0` → `1.0.1-SNAPSHOT`
7. Run `git commit && git push`

Miss any step → broken release. Do this 50 times a year → 50 chances for human error.

### What the Unleash Maven Plugin Does

The `unleash-maven-plugin` automates the entire release lifecycle in a **single command**:

```bash
mvn unleash:perform
```

Internally it executes this workflow automatically:

```
checkProjectVersions → checkDependencies → setReleaseVersions
        ↓
buildReleaseArtifacts → tagScm → deployArtifacts
        ↓
setDevVersion → pushChanges
```

### Why Unleash Over maven-release-plugin?

| Feature | maven-release-plugin | unleash-maven-plugin |
|---------|---------------------|----------------------|
| Resumable on failure | ❌ | ✅ |
| Single command | ❌ Two commands | ✅ One command |
| Dry-run mode | Limited | ✅ Full dry-run |
| Atomic commit strategy | ❌ | ✅ |
| Still actively maintained | ⚠️ Slow | ✅ Active community |

---

## 2. Prerequisites

| Tool | Minimum Version | Notes |
|------|----------------|-------|
| Java | 11+ | Java 21 used in this project |
| Maven | 3.3.9+ | Must be on `$PATH` |
| Git | Any modern | Repo must be initialised |
| GitHub Account | — | PAT token required |

---

## 3. Plugin Integration into pom.xml

### 3.1 — Required Blocks

The plugin needs **three things** in your `pom.xml`:

#### A — The `<scm>` Block

Tells the plugin where your Git repository lives. **Must match your actual remote.**

As configured in [device-management-service](https://github.com/tsjahangiri/device-management-service):

```xml
<scm>
    <connection>
        scm:git:https://github.com/tsjahangiri/device-management-service.git
    </connection>
    <developerConnection>
        scm:git:https://github.com/tsjahangiri/device-management-service.git
    </developerConnection>
    <url>https://github.com/tsjahangiri/device-management-service</url>
    <tag>HEAD</tag>
</scm>
```

> Replace `tsjahangiri/device-management-service` with your own `YOUR_ORG/YOUR_REPO` when adapting for a different project.

#### B — The `<distributionManagement>` Block

Tells the plugin where to deploy the built JAR.

As configured in [device-management-service](https://github.com/tsjahangiri/device-management-service):

```xml
<distributionManagement>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>
            https://maven.pkg.github.com/tsjahangiri/device-management-service
        </url>
    </repository>
    <snapshotRepository>
        <id>github</id>
        <name>GitHub Packages (Snapshots)</name>
        <url>
            https://maven.pkg.github.com/tsjahangiri/device-management-service
        </url>
    </snapshotRepository>
</distributionManagement>
```

#### C — The Plugin Block Inside `<build><plugins>`

```xml
<properties>
    <unleash.version>3.3.0</unleash.version>
</properties>

<!-- inside <build><plugins> -->
<plugin>
    <groupId>io.github.mavenplugins</groupId>
    <artifactId>unleash-maven-plugin</artifactId>
    <version>${unleash.version}</version>

    <dependencies>
        <!-- Git SCM provider — version must match plugin version exactly -->
        <dependency>
            <groupId>io.github.mavenplugins</groupId>
            <artifactId>unleash-scm-provider-git</artifactId>
            <version>${unleash.version}</version>
        </dependency>
    </dependencies>

    <configuration>
        <!-- Tag format: device-service-0.0.1 -->
        <tagNamePattern>
            @{project.artifactId}-@{project.version}
        </tagNamePattern>

        <!-- Prefix on all git commits made by the plugin -->
        <scmMessagePrefix>[unleash-release]</scmMessagePrefix>

        <!--
            Blocks release if any dependency is still a SNAPSHOT.
            Keep false for production safety.
        -->
        <allowLocalReleaseArtifacts>false</allowLocalReleaseArtifacts>
    </configuration>
</plugin>
```

> ⚠️ **Critical:** Do NOT add the unleash plugin as a `<dependency>` inside the `<dependencies>` section. It belongs only in `<build><plugins>`. Adding it as a dependency bundles the entire plugin JAR into your application.

---

### 3.2 — Version Source of Truth

Unleash reads `pom.xml` version only — it does **not** read GitHub tags to determine the next version.

```
pom.xml says:   0.0.3-SNAPSHOT
                     ↓
Unleash releases  →  0.0.3
Unleash sets      →  0.0.4-SNAPSHOT  (automatic)
                     ↓
Next run pom.xml: 0.0.4-SNAPSHOT  ← unleash already set this
```

The version in `pom.xml` is the **single source of truth**. After each release, unleash bumps it to the next patch SNAPSHOT automatically.

---

## 4. One-Time Setup — Git & Credentials

### 4.1 — Initialise the Git Repository

As done for [device-management-service](https://github.com/tsjahangiri/device-management-service):

```bash
git init
git add .
git commit -m "initial commit"
git remote add origin https://github.com/tsjahangiri/device-management-service.git
git push -u origin main
```

> ⚠️ The working directory must be **clean** (no uncommitted changes) before running unleash.

---

### 4.2 — Create a GitHub Personal Access Token (PAT)

1. Go to → **https://github.com/settings/tokens**
2. Click **"Generate new token (classic)"**
3. Name it: `unleash-maven-release`
4. Select scopes:
   - ✅ `repo` (full repository access)
   - ✅ `write:packages`
   - ✅ `read:packages`
5. Copy the token — GitHub shows it **only once**

---

### 4.3 — Configure `~/.m2/settings.xml`

Create or edit `~/.m2/settings.xml`:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <servers>

    <!--
      For JGit SCM operations (push commits and tags to GitHub).
      The id must match the HOSTNAME of your SCM URL.
    -->
    <server>
      <id>github.com</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>ghp_yourPersonalAccessTokenHere</password>
    </server>

    <!--
      For Maven artifact deployment to GitHub Packages.
      The id must match <distributionManagement><repository><id>.
    -->
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>ghp_yourPersonalAccessTokenHere</password>
    </server>

  </servers>
</settings>
```

> The two server entries serve different purposes:
> - `github.com` → used by JGit to push commits and tags
> - `github` → used by Maven to upload the JAR to GitHub Packages

---

## 5. Full Release Cycle Walkthrough

### 5.1 — Before You Start, Verify State

```bash
# 1. Git repo is clean
git status
# Expected: nothing to commit, working tree clean

# 2. Remote is configured
git remote -v
# Expected: origin https://github.com/YOUR_ORG/YOUR_REPO.git

# 3. pom.xml version ends in -SNAPSHOT
grep "<version>" pom.xml | head -3
# Expected: <version>0.0.3-SNAPSHOT</version>

# 4. Preview what versions would be calculated
mvn unleash:releaseVersion
# Output: 0.0.3

mvn unleash:nextSnapshotVersion
# Output: 0.0.4-SNAPSHOT
```

---

### 5.2 — Dry Run (Always Do This First)

```bash
mvn unleash:perform -Dunleash.dryRun=true
```

Expected output:
```
[INFO] Would set release version: 0.0.3
[INFO] Would create tag: device-service-0.0.3
[INFO] Would set next dev version: 0.0.4-SNAPSHOT
[INFO] BUILD SUCCESS
```

No files are changed. No Git commits made. Zero risk.

---

### 5.3 — Perform the Real Release

As executed in [device-management-service](https://github.com/tsjahangiri/device-management-service):

```bash
mvn unleash:perform \
  -Dunleash.scmUsername=YOUR_GITHUB_USERNAME \
  -Dunleash.scmPassword=ghp_yourToken
```

What happens automatically:

```
pom.xml: 0.0.3-SNAPSHOT → 0.0.3          ← git commit pushed
                            ↓
                       mvn deploy          ← JAR deployed to GitHub Packages
                            ↓
              git tag device-service-0.0.3 ← pushed to GitHub
                            ↓
pom.xml: 0.0.3 → 0.0.4-SNAPSHOT          ← git commit pushed
```

---

### 5.4 — Verify the Release on GitHub

After a successful release you should see in the [device-management-service](https://github.com/tsjahangiri/device-management-service) repository:

```
GitHub Tags:     device-service-0.0.3  ✅
GitHub Packages: device-service-0.0.3.jar  ✅
pom.xml:         <version>0.0.4-SNAPSHOT</version>  ✅
```

> **Note:** GitHub Releases section will be empty — unleash creates Git tags, not GitHub Releases. To create a GitHub Release, do it manually in the GitHub UI or set up a GitHub Actions workflow triggered by the tag push.

---

### 5.5 — The Ongoing Cycle

Once set up, the pattern repeats forever with no manual version editing:

```
pom.xml: 0.0.4-SNAPSHOT  ← unleash already set this after last release
              ↓
mvn unleash:perform       ← your only command
              ↓
Released: 0.0.4
pom.xml:  0.0.5-SNAPSHOT  ← unleash sets this automatically
              ↓
(repeat forever)
```

---

## 6. Pitfalls & Fixes Encountered

These are real issues hit during the integration of this plugin into the [`device-management-service`](https://github.com/tsjahangiri/device-management-service) project.

---

### Pitfall 1 — Wrong GroupId (Plugin Not Found in Maven Central)

**Error:**
```
Could not find artifact com.itemis.maven.plugins:unleash-maven-plugin:pom:3.0.0
```

**Cause:** The original author abandoned the plugin in 2022. The community adopted it and moved it to a new groupId. The old `com.itemis.maven.plugins` groupId no longer publishes new versions to Maven Central.

**Fix:** Use the new groupId and artifact names:

```xml
<!-- ❌ Old — does not exist in Maven Central for v3+ -->
<groupId>com.itemis.maven.plugins</groupId>
<artifactId>unleash-maven-plugin</artifactId>
<!-- dependency: scm-provider-jgit -->

<!-- ✅ New — correct, actively maintained -->
<groupId>io.github.mavenplugins</groupId>
<artifactId>unleash-maven-plugin</artifactId>
<!-- dependency: unleash-scm-provider-git -->
```

---

### Pitfall 2 — SCM Provider Version Mismatch

**Error:**
```
No SCM provider found for SCM with name git.
```

**Cause:** Plugin version (`3.3.0`) and SCM provider version (`3.3.1`) did not match. They must be identical.

**Fix:**
```xml
<plugin>
    <version>3.3.0</version>
    <dependencies>
        <dependency>
            <artifactId>unleash-scm-provider-git</artifactId>
            <version>3.3.0</version>  <!-- ← must match exactly -->
        </dependency>
    </dependencies>
</plugin>
```

---

### Pitfall 3 — Git Repository Not Initialised

**Error:**
```
One of setGitDir or setWorkTree must be called.
```

**Cause:** The project folder had no `.git` directory. Unleash's JGit library cannot find the repository.

**Fix:**
```bash
git init
git add .
git commit -m "initial commit"
git remote add origin https://github.com/YOUR_ORG/YOUR_REPO.git
git push -u origin main
```

---

### Pitfall 4 — Authentication Failure When Pushing Tags

**Error:**
```
Authentication is required but no CredentialsProvider has been registered
```

**Cause:** JGit (the Git library inside unleash) does not read system Git credentials or the OS keychain. It needs credentials passed explicitly.

**Fix — Option A (command line):**
```bash
mvn unleash:perform \
  -Dunleash.scmUsername=YOUR_USERNAME \
  -Dunleash.scmPassword=ghp_yourToken
```

**Fix — Option B (`settings.xml`, permanent):**
```xml
<server>
    <id>github.com</id>  <!-- must be hostname, not arbitrary name -->
    <username>YOUR_USERNAME</username>
    <password>ghp_yourToken</password>
</server>
```

---

### Pitfall 5 — `<developmentVersion>auto</developmentVersion>` Bug

**Error:**
```
com.device.management:device-service:auto
There are no snapshot projects that could be released!
```

**Cause:** `auto` is not a valid value for `<developmentVersion>`. Unleash took it literally and set `pom.xml` version to the string `"auto"` instead of calculating the next version.

**Fix:** Remove `<developmentVersion>` entirely — unleash auto-calculates the next patch version without any configuration:

```xml
<configuration>
    <tagNamePattern>@{project.artifactId}-@{project.version}</tagNamePattern>
    <scmMessagePrefix>[unleash-release]</scmMessagePrefix>
    <!-- ✅ No <developmentVersion> needed -->
</configuration>
```

Then manually restore `pom.xml`:
```xml
<!-- Fix the version that got set to "auto" -->
<version>0.0.4-SNAPSHOT</version>
```

---

### Pitfall 6 — `unleash:rollback` Goal No Longer Exists

**Error:**
```
Could not find goal 'rollback' in plugin io.github.mavenplugins:unleash-maven-plugin:3.3.0
among available goals: nextSnapshotVersion, perform, perform-tycho, releaseVersion
```

**Cause:** The `rollback` goal existed in the old `com.itemis.maven.plugins` version but was not carried over to `io.github.mavenplugins`.

**Fix — Manual Rollback:**

```bash
# Step 1 — Find the commit before unleash touched anything
git log --oneline
# abc1234  [unleash-release] set development version to 0.0.4-SNAPSHOT
# def5678  [unleash-release] set release version to 0.0.3
# ghi9012  your last real commit  ← reset to here

# Step 2 — Reset to before unleash commits
git reset --hard ghi9012

# Step 3 — Force push
git push origin main --force

# Step 4 — Delete the remote tag
git push origin :refs/tags/device-service-0.0.3

# Step 5 — Delete local tag
git tag -d device-service-0.0.3
```

> ⚠️ **Important:** Rollback only undoes Git changes. If the JAR was already deployed to GitHub Packages or Nexus, it **cannot be deleted programmatically** — Maven artifact repositories are append-only by design. The correct response to a bad release is to release a fixed version immediately (e.g. `0.0.4`) and notify the team to skip the bad version.

---

### Pitfall 7 — Unrelated Git Histories After `git init`

**Error (on GitHub):**
```
There isn't anything to compare.
main and working are entirely different commit histories.
```

**Cause:** Running `git init` locally created a brand new history with no connection to the existing GitHub repo.

**Fix:**
```bash
# If GitHub only has auto-generated files (README) — force push local history
git push origin main --force

# If both histories have important commits — merge them
git pull origin main --allow-unrelated-histories
git push origin main
```

---

## 7. Debugging How-To — Attaching IntelliJ Debugger

This section explains how to attach a live debugger to the unleash plugin during execution to inspect its internal logic.

### 7.1 — How It Works

```
Normal:    mvn unleash:perform
           → starts immediately → runs → finishes

Debug:     mvnDebug unleash:perform
           → starts → FREEZES → prints port 8000 → waits...
           → you connect IntelliJ debugger
           → execution resumes
           → pauses at your breakpoints
```

### 7.2 — Step 1: Get the Plugin Source Code

```bash
# Clone the plugin source (outside your project folder)
git clone https://github.com/mavenplugins/unleash-maven-plugin.git

# Also clone the SCM provider (handles all Git operations)
git clone https://github.com/mavenplugins/unleash-scm-provider-git.git
```

---

### 7.3 — Step 2: Open Plugin Source in IntelliJ

```
File → Project Structure → Modules → + → Import Module
→ Select: unleash-maven-plugin/pom.xml
```

This lets IntelliJ show you the actual plugin source when execution pauses at a breakpoint.

---

### 7.4 — Step 3: Create a Remote Debug Configuration

```
Run → Edit Configurations → + → Remote JVM Debug

Name:           Debug Unleash Plugin
Debugger mode:  Attach to remote JVM
Host:           localhost
Port:           8000
```

Click **OK**.

---

### 7.5 — Step 4: Set Breakpoints

Open the plugin source and set breakpoints on lines of interest:

| File | Purpose | When to Break |
|------|---------|---------------|
| `TagScm.java` | Git tag creation | Debugging tag push failures |
| `ScmProviderGit.java:519` | Credential handling | Authentication issues |
| `CheckProjectVersions.java` | Version validation | Version format errors |
| `DevVersionUtil.java` | Next version calculation | Wrong version being set |
| `DeployArtifacts.java` | JAR deployment | Nexus/GitHub Packages errors |

---

### 7.6 — Step 5: Run `mvnDebug` in Terminal

```bash
cd /path/to/device-management-service
# Repo: https://github.com/tsjahangiri/device-management-service

mvnDebug unleash:perform \
  -Dunleash.scmUsername=YOUR_GITHUB_USERNAME \
  -Dunleash.scmPassword=ghp_yourToken
```

Terminal will show and **freeze**:
```
Preparing to execute Maven in debug mode
Listening for transport dt_socket at address: 8000
```

> Do not close this terminal. Maven is paused waiting for your IDE.

---

### 7.7 — Step 6: Attach the Debugger

In IntelliJ:
1. Select **"Debug Unleash Plugin"** config from the dropdown (top right)
2. Click the **green bug icon 🐛** (Debug button — NOT the play button)
3. IntelliJ bottom panel shows: `Connected to the target VM, address: 'localhost:8000'`

Maven unfreezes and runs. When it hits your breakpoint — it **pauses** and you can inspect all variables live.

---

### 7.8 — What You See When Paused

```
┌─────────────────────────┬────────────────────────────────────┐
│ FRAMES (call stack)     │ VARIABLES                          │
├─────────────────────────┼────────────────────────────────────┤
│ TagScm.execute()        │ tagName = "device-service-0.0.3"   │
│ WorkflowExecutor        │ scmRevision = "abc123..."          │
│ AbstractCDIMojo         │ credentialsProvider = null ← !!    │
└─────────────────────────┴────────────────────────────────────┘
```

Debugger toolbar:

| Button | Action |
|--------|--------|
| ▶ Resume | Continue to next breakpoint |
| ⬇ Step Over | Execute current line, move to next |
| ⬇ Step Into | Go inside the method being called |
| ⬆ Step Out | Finish current method, return to caller |

---

### 7.9 — Alternative: If `mvnDebug` Is Not Found

```bash
# Set MAVEN_OPTS manually instead
export MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
mvn unleash:perform \
  -Dunleash.scmUsername=YOUR_GITHUB_USERNAME \
  -Dunleash.scmPassword=ghp_yourToken

# Then connect IntelliJ to port 5005 instead of 8000
```

---

## 8. Available Goals Reference

The `io.github.mavenplugins` version provides these goals:

| Goal | Command | What It Does |
|------|---------|-------------|
| `perform` | `mvn unleash:perform` | Full release cycle |
| `releaseVersion` | `mvn unleash:releaseVersion` | **Print only** — what release version would be |
| `nextSnapshotVersion` | `mvn unleash:nextSnapshotVersion` | **Print only** — what next SNAPSHOT would be |
| `perform-tycho` | `mvn unleash:perform-tycho` | Release for Eclipse Tycho/OSGi projects |

> ⚠️ The `rollback` goal from the old `com.itemis.maven.plugins` version **no longer exists**. See [Pitfall 6](#pitfall-6--unleashrollback-goal-no-longer-exists) for the manual rollback procedure.

---

## 9. Lessons Learned & Recommendations

### For Teams Adopting This Plugin

**1. Always dry-run before releasing**
```bash
mvn unleash:perform -Dunleash.dryRun=true
```
Takes 10 seconds. Prevents bad releases.

**2. Never release from a local machine in production**

Set up GitHub Actions to trigger releases. Human error is eliminated, credentials are managed safely, and Docker/databases are available for Testcontainer tests.

**3. There is no real rollback once a JAR hits a registry**

Maven artifact repositories are append-only. Plan for this: fix forward with a new version rather than trying to delete a bad one.

**4. The version in `pom.xml` is the single source of truth**

Unleash does NOT read GitHub tags to determine version numbers. The `pom.xml` version tells unleash everything it needs. After each release, unleash updates it automatically — never touch it manually again.

**5. Keep `<developmentVersion>` out of your config**

Do not add `<developmentVersion>` to the plugin configuration. Unleash calculates the next patch version automatically. Any value you put there is taken as a literal string.

**6. The `allowLocalReleaseArtifacts` flag is a safety net**

Keep it `false` in production. It blocks the release if any dependency is still a SNAPSHOT, which is the correct behaviour.

---

### Summary of Plugin Evaluation

| Criterion | Assessment |
|-----------|-----------|
| Technical viability | ✅ Solid — handles full release lifecycle reliably |
| Ease of setup | ⚠️ Moderate — groupId migration caused initial confusion |
| Debuggability | ✅ Excellent — `mvnDebug` + IntelliJ gives full visibility |
| CI/CD compatibility | ✅ Works well with GitHub Actions |
| Multi-module support | ✅ Within a single repo |
| Multi-repo support | ❌ Requires custom orchestration (see Task 2) |
| Active maintenance | ✅ `io.github.mavenplugins` community is active |
| Rollback capability | ⚠️ Git-only rollback; artifacts in registry are permanent |

**Verdict:** The `unleash-maven-plugin` is a technically sound replacement for manual version bumping and `maven-release-plugin`. The one-time setup investment is worthwhile for any team running frequent releases.
