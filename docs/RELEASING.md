# Releasing

1. Update `CHANGELOG.md` and remove `Unreleased` from the target version.
2. Run `./gradlew clean allTests lintRelease apiCheck dokkaGenerate verifyPublishedConsumers`.
3. Verify the generated POM and Gradle module metadata in the temporary repository.
4. Tag the commit as `vX.Y.Z`. CI derives the publication version from `GITHUB_REF_NAME`; local builds can use `-PVERSION_NAME=X.Y.Z`.
5. Push the tag: `git push origin vX.Y.Z`. The `Release` workflow publishes to
   Maven Central and releases automatically (`automaticRelease = true`), so no
   manual Central Portal action is required.
6. Build the iOS sample against the pinned Microsoft Clarity SPM release before closing the release.

Never add the Microsoft XCFramework to the Maven publication or repository.

## Required CI secrets

The `Release` workflow needs the following repository secrets (**Settings →
Secrets and variables → Actions → New repository secret**):

| Secret | Source |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Central Portal user token name (https://central.sonatype.com → Account → User Token) |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user token password |
| `SIGNING_KEY_ID` | Last 8 chars of the GPG key fingerprint (`gpg --list-secret-keys --keyid-format=long`) |
| `SIGNING_KEY` | The GPG private key as the **raw ASCII-armored text** — `gpg --armor --export-secret-keys KEYID`. Do **not** base64-encode it: Gradle's in-memory PGP provider feeds the value directly to the armor decoder, so the `-----BEGIN PGP PRIVATE KEY BLOCK-----` header must be present (line breaks included). Store the value with its newlines intact. |
| `SIGNING_KEY_PASSWORD` | Password protecting the GPG key |

> **Note:** the Vanniktech plugin's KDoc suggests stripping the armor header/footer and line breaks from the key. That applies to older Gradle versions; Gradle 9.4's `useInMemoryPgpKeys` requires the full armored block (headers + newlines). If the publish fails with `Could not read PGP secret key` or `no configured signatory`, first re-check this secret's format.

The workflow maps these to the Gradle properties the Vanniktech plugin expects
(`ORG_GRADLE_PROJECT_mavenCentralUsername`, `signingInMemoryKeyId`, etc.). For
local validation of the signing/publishing step, export the same names as
environment variables or set them in `~/.gradle/gradle.properties` and run
`./gradlew :clarity-kmp:publishAllPublicationsToMavenCentral`.

## Clarity iOS SDK upgrade gate

The iOS interop ABI shim (`clarity-kmp/src/nativeInterop/cinterop/headers/ClarityInterop.h`)
targets a **pinned** Microsoft Clarity iOS SDK version (see the `TARGETS:` comment
in that header and the `clarity-ios-sdk` entry in `gradle/libs.versions.toml`).
Because the shim is hand-written and linked with `-undefined dynamic_lookup`, a
selector/return-type mismatch compiles but fails at runtime. **Before releasing
any change to the `clarity-ios-sdk` version**, follow the upgrade procedure in
`clarity-kmp/src/nativeInterop/cinterop/headers/README.md` and confirm the
selectors still match the real `-Swift.h`.
