---
name: publish-maven
description: Publish reown-kotlin SDK artifacts to Maven Central via Sonatype, and bump module versions. Use when releasing a new SDK version, running publishToSonatype or closeAndReleaseSonatypeStagingRepository, bumping versions in Versions.kt, or setting up the required signing/portal credentials.
---

# Publishing to Maven Central

**Repository**: Maven Central via Sonatype OSSRH

## Version Management

Versions are centralized in `buildSrc/src/main/kotlin/Versions.kt` — all modules share the same version. Dependency versions live in the `gradle/libs.versions.toml` version catalog.

## Artifacts published

| Module | Group | Artifact ID |
|--------|-------|-------------|
| Foundation | com.reown | foundation |
| Core | com.reown | android-core |
| Sign | com.reown | sign |
| Notify | com.reown | notify |
| WalletKit | com.reown | walletkit |
| AppKit | com.reown | appkit |
| Modal Core | com.reown | modal-core |
| BOM | com.reown | android-bom |

## Publishing commands

```bash
# Publish to staging
./gradlew publishToSonatype

# Close and release staging repository
./gradlew closeAndReleaseSonatypeStagingRepository

# Bump versions (custom task)
./gradlew bumpVersion -PnewVersion=1.6.0
```

## Required environment variables

- `CENTRAL_PORTAL_USERNAME` - Sonatype username
- `CENTRAL_PORTAL_PASSWORD` - Sonatype password
- `SIGNING_KEY_ID` - GPG key ID
- `SIGNING_KEY` - GPG private key (armored)
- `SIGNING_PASSWORD` - GPG key passphrase

## Publication includes

- Release AAR/JAR
- Sources JAR (`sourcesJar`)
- Javadoc JAR (`javadocJar` via Dokka)
- POM with license, developer, and SCM information

## Version Bumping Workflow

1. Update versions in `buildSrc/src/main/kotlin/Versions.kt`
2. Run `./gradlew build` to verify
3. Commit with message: `chore: bump version to X.Y.Z`
4. Tag release: `git tag vX.Y.Z`
5. Push tag to trigger CI publish
