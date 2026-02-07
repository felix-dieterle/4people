# Build and Release Workflow Guide

## Overview

This document explains the automated build and release workflows for the 4people Android app.

## Workflows

### PR Build Check (`pr-build.yml`)

Runs on every pull request to `main` branch to catch build issues early.

**Features:**
1. **Builds a release APK** to verify the code compiles
2. **Uploads the APK as an artifact** for testing (retained for 7 days)
3. **Generates debug keystore** automatically for signing

**When it runs:** On every pull request to `main`

**Purpose:** Catch build problems before merging to main

### Release Workflow (`release.yml`)

Runs when code is merged to `main` branch to create official releases, or can be triggered manually.

**Features:**
1. **Increments the version** (both `versionCode` and `versionName`)
2. **Builds a release APK** using Gradle
3. **Creates a GitHub release** with the version tag
4. **Uploads the APK** to the release
5. **Saves the APK as a workflow artifact** for 90 days
6. **Generates debug keystore** automatically for signing

**When it runs:** 
- Automatically on every push to `main` branch
- Manually via the GitHub Actions UI (workflow_dispatch)

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

### Workflow Steps (Release)

1. **Checkout code**: Fetches the repository
2. **Set up JDK 17**: Configures Java for Android builds
3. **Generate debug keystore**: Creates Android debug keystore for signing
4. **Get current version**: Extracts version from `app/build.gradle.kts`
5. **Increment version**: Calculates new version numbers
6. **Commit version bump**: Commits updated version to main branch
7. **Build Release APK**: Runs `./gradlew assembleRelease`
8. **Find APK**: Locates the built APK file
9. **Create Release**: Creates GitHub release with tag and uploads APK
10. **Upload artifact**: Saves APK as workflow artifact

### Workflow Steps (PR Build)

1. **Checkout code**: Fetches the repository
2. **Set up JDK 17**: Configures Java for Android builds
3. **Generate debug keystore**: Creates Android debug keystore for signing
4. **Build Release APK**: Runs `./gradlew assembleRelease`
5. **Upload artifact**: Saves APK as workflow artifact for testing

## Testing the Workflows

### Testing PR Builds

1. **Create a pull request** to the `main` branch:
   ```bash
   git checkout -b my-feature
   # Make your changes
   git commit -am "My changes"
   git push origin my-feature
   # Create PR via GitHub UI
   ```

2. **Monitor the PR build**:
   - Check the PR page for the "PR Build Check" status
   - Click "Details" to see the workflow run
   - Watch the build progress

3. **Download the test APK**:
   - Go to the workflow run details
   - Find "Artifacts" section at the bottom
   - Download "pr-build-apk"
   - Install on Android device for testing

### Testing Release Workflow

There are two ways to trigger the release workflow:
1. **Automatically** - by merging/pushing to the `main` branch
2. **Manually** - using the GitHub Actions UI

#### Manual Triggering

To manually trigger a release build:

1. **Navigate to Actions tab** in the GitHub repository
2. **Select "Build and Release APK"** from the workflows list
3. **Click "Run workflow"** button (top right)
4. **Select the branch** (usually `main`)
5. **Click "Run workflow"** to start the build

This is useful for:
- Creating a release without making code changes
- Re-running a failed release build
- Testing the release workflow

#### Prerequisites

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
- Ensure debug keystore generation step succeeded

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

- The **PR build workflow** runs on every pull request to catch issues early
- The **release workflow** only runs on pushes to `main` branch
- Each merge creates exactly one release
- APK artifacts from releases are retained for 90 days
- APK artifacts from PR builds are retained for 7 days
- **Release APKs are signed with the Android debug keystore** (generated automatically in CI) to make them installable
- The debug keystore is generated using standard Android debug key values for consistency
- For production releases with Google Play Store, replace the debug signing configuration with a proper release keystore
