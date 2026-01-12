# Release Workflow Testing Guide

## Overview

This document explains how to test the automated release workflow that builds and publishes APKs when code is merged to the `main` branch.

## Workflow Features

The release workflow (`release.yml`) automatically:

1. **Increments the version** (both `versionCode` and `versionName`)
2. **Builds a release APK** using Gradle
3. **Creates a GitHub release** with the version tag
4. **Uploads the APK** to the release
5. **Saves the APK as a workflow artifact** for 90 days

## How It Works

### Version Increment Logic

- **versionCode**: Incremented by 1 (e.g., 1 → 2 → 3)
- **versionName**: Follows semantic versioning
  - For X.Y.Z format: Patch is incremented (e.g., 1.0.0 → 1.0.1)
  - For X.Y format: Minor is incremented and patch is set to 0 (e.g., 1.0 → 1.1.0)

### Infinite Loop Prevention

The workflow includes a check to prevent infinite loops:
```yaml
if: "!contains(github.event.head_commit.message, 'Bump version to')"
```

This ensures the workflow doesn't trigger itself when it commits the version bump.

### Workflow Steps

1. **Checkout code**: Fetches the repository
2. **Set up JDK 17**: Configures Java for Android builds
3. **Get current version**: Extracts version from `app/build.gradle.kts`
4. **Increment version**: Calculates new version numbers
5. **Commit version bump**: Commits updated version to main branch
6. **Build Release APK**: Runs `./gradlew assembleRelease`
7. **Find APK**: Locates the built APK file
8. **Create Release**: Creates GitHub release with tag and uploads APK
9. **Upload artifact**: Saves APK as workflow artifact

## Testing the Workflow

### Prerequisites

- Push access to the `main` branch
- GitHub Actions enabled for the repository

### Test Procedure

1. **Merge a PR to main** or **push directly to main**:
   ```bash
   git checkout main
   git merge your-feature-branch
   git push origin main
   ```

2. **Monitor the workflow**:
   - Go to Actions tab in GitHub
   - Look for "Build and Release APK" workflow
   - Watch the workflow progress through all steps

3. **Verify the outputs**:
   - Check that `app/build.gradle.kts` has updated version numbers
   - Verify a new release appears in the Releases section
   - Download and inspect the APK from the release
   - Check that the workflow artifact was created

### Expected Results

After a successful merge to main:

- ✅ Version in `app/build.gradle.kts` is incremented
- ✅ New commit on main: "Bump version to X.Y.Z (build N)"
- ✅ New release tag: `vX.Y.Z`
- ✅ Release has attached APK: `4people-vX.Y.Z.apk`
- ✅ Workflow artifact available for download

### Manual Testing of APK

1. Download the APK from the release
2. Install on an Android device (enable "Install from Unknown Sources")
3. Verify the app version in Android Settings → Apps → 4people

### Troubleshooting

**Workflow doesn't trigger:**
- Ensure push is to `main` branch
- Check GitHub Actions is enabled
- Verify workflow file is in `.github/workflows/`

**Build fails:**
- Check JDK version (should be 17)
- Verify Gradle wrapper files are present
- Check Android SDK availability in runner

**Version not incrementing:**
- Verify version format in `app/build.gradle.kts`
- Check sed command syntax in workflow

**Infinite loop detected:**
- Verify the `if` condition is working
- Check commit message doesn't manually contain "Bump version to"

## Manual Build (Local Testing)

To test the build process locally:

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Check version
grep -A 1 "versionCode\|versionName" app/build.gradle.kts
```

## Notes

- The workflow only runs on pushes to `main` branch
- Each merge creates exactly one release
- APK artifacts are retained for 90 days
- Release APKs are unsigned (for development/testing)
- For production releases, consider adding APK signing
