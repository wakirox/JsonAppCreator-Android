# Releasing Appkin

Releases are built by `.github/workflows/release.yml`. The workflow tests the tagged
source, validates the public example configuration, builds a signed release APK,
verifies its signature, creates a SHA-256 checksum, and publishes both files on the
matching GitHub Release.

## Required GitHub secrets

Configure these repository Actions secrets:

- `ANDROID_KEYSTORE_BASE64`: the complete signing keystore, Base64-encoded as one line
- `ANDROID_KEYSTORE_PASSWORD`: the keystore password
- `ANDROID_KEY_ALIAS`: the signing key alias
- `ANDROID_KEY_PASSWORD`: the signing key password

An optional `TMDB_API_KEY` repository secret supplies the TMDB key directly to both
CI and release builds. For multiple bundled configurations, the optional
`APP_CONFIG_KEYS_BASE64` secret can provide Base64-encoded, per-configuration API-key
properties. `TMDB_API_KEY` takes precedence if both sources define the TMDB key.
These values are compiled into the APK and are recoverable by anyone who downloads
it, so use only restricted client keys.

The release workflow fails if any value is missing. It decodes the keystore only into
the runner's temporary directory and removes it after the job. Never commit a
keystore, Base64 keystore data, passwords, or locally signed APKs.

## Create and store a signing keystore

Create a dedicated release key on a trusted, offline-capable workstation. Let
`keytool` prompt for unique password-manager-generated secrets; do not put passwords
on the command line:

```sh
keytool -genkeypair -v \
  -keystore json-app-creator-release.keystore \
  -alias json-app-creator-release \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Keep encrypted backups of the original keystore and its passwords in at least two
separate secure locations. Restrict the local file before encoding it:

```sh
chmod 600 json-app-creator-release.keystore
base64 -w 0 json-app-creator-release.keystore > json-app-creator-release.keystore.base64
```

On macOS, where `base64 -w` is unavailable, use:

```sh
base64 -i json-app-creator-release.keystore | tr -d '\n' \
  > json-app-creator-release.keystore.base64
```

Add each value under **Repository settings → Secrets and variables → Actions**. The
GitHub CLI can upload the encoded file without printing it:

```sh
gh secret set ANDROID_KEYSTORE_BASE64 < json-app-creator-release.keystore.base64
gh secret set ANDROID_KEYSTORE_PASSWORD
gh secret set ANDROID_KEY_ALIAS
gh secret set ANDROID_KEY_PASSWORD
gh secret set TMDB_API_KEY
```

The last four commands prompt for their values. Delete the temporary Base64 file
after the secret is stored, and never paste secret values into command arguments,
shell history, workflow logs, issues, or chat.

## Version and tag strategy

Release tags must use `vMAJOR.MINOR.PATCH`, for example `v1.0.0`. The workflow removes
the leading `v` for Android `versionName`. It derives Android `versionCode` as:

```text
MAJOR * 1,000,000 + MINOR * 1,000 + PATCH
```

Minor and patch values must each be between 0 and 999, and the final code must be
between 1 and 2,100,000,000. This encoding is deterministic and preserves semantic
version ordering. Local builds default to `versionName` `1.0` and `versionCode` `1`;
CI can override them with `APP_VERSION_NAME` and `APP_VERSION_CODE`.

Create a release only after the release workflow is present on the tagged commit and
the `master` build is green:

```sh
git switch master
git pull --ff-only origin master
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

Pushing the tag starts the workflow. The manual workflow input is intended for an
existing tag, such as retrying publication after correcting repository secrets. A
rerun updates existing release assets instead of creating a duplicate release.

## Verify a downloaded release

Download the APK and its `.sha256` file from the same GitHub Release, then run:

```sh
sha256sum --check Appkin-v1.0.0.apk.sha256
```

On macOS, use `shasum -a 256` and compare its output with the checksum file. To verify
the Android signature with installed Android SDK build tools:

```sh
apksigner verify --verbose --print-certs Appkin-v1.0.0.apk
```

Compare the certificate digest with a separately recorded trusted digest from a
known-good release, not only with information from the same download location.

## Key rotation and recovery

For routine secret rotation, upload a newly encoded copy of the exact same keystore
and update all four GitHub secrets together. Run the workflow manually against an
existing test tag or create the next release tag, then compare the signing certificate
digest with the trusted digest before distributing the APK.

Do not replace the signing certificate casually. For APKs distributed directly from
GitHub, a different signing key generally prevents Android from installing the new APK
as an update to the existing application, especially across the full supported API
range. If a true certificate migration is unavoidable, design and test an Android
signing-lineage migration before changing repository secrets.

**Losing the signing keystore or its passwords prevents compatible updates signed as
the existing application.** Treat the encrypted backups and recovery instructions as
critical release infrastructure.
