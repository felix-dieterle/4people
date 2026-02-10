# Release Version Robustness

## Problem

Previously, the release workflow would only look at the version in `app/build.gradle.kts` to determine the next version. This caused issues when:
- The version in code was manually changed to be older than the latest release
- Manual changes were made that got the version out of sync with releases
- This would lead to version conflicts where the workflow would try to create a release with a version that already existed

## Solution

The release workflow now:

1. **Reads version from code** (`app/build.gradle.kts`)
2. **Fetches the latest release version from GitHub** via the GitHub API
3. **Compares both versions** and uses the maximum version as the base
4. **Increments from the higher version** to ensure no conflicts

### Example Scenarios

#### Scenario 1: Code version behind release (the original problem)
- Code: `1.0.38` (build 39)
- Latest Release: `1.0.48` (build 49)
- **Next Release**: `1.0.49` (build 50) ✅

#### Scenario 2: Code version ahead of release
- Code: `1.0.55` (build 56)
- Latest Release: `1.0.48` (build 49)
- **Next Release**: `1.0.56` (build 57) ✅

#### Scenario 3: No releases exist yet
- Code: `1.0.0` (build 1)
- Latest Release: None
- **Next Release**: `1.0.1` (build 2) ✅

## Benefits

- **No version conflicts**: Always creates a new, unique version
- **Robust against manual changes**: Works even if someone manually changes the version
- **Idempotent**: Can be run multiple times safely
- **Automatic recovery**: Self-corrects version mismatches

## Technical Details

The workflow uses three steps:

1. `Get current version from code` - Extracts version from build.gradle.kts
2. `Get latest release version from GitHub` - Fetches the latest release via GitHub API
3. `Determine next version` - Compares both and increments from the maximum

The version code (build number) is always incremented by 1 from the base.
The version name follows semantic versioning, incrementing the patch number.
