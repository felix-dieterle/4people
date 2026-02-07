# Release Keystore Setup Guide

## Why This Matters

For Android APKs to be updateable, **all versions must be signed with the same keystore**. Without this:
- ❌ Users cannot update the app - they must uninstall and reinstall
- ❌ All user data and settings are lost during reinstall
- ❌ The app appears as a completely different application to Android

With proper keystore management:
- ✅ Users can seamlessly update to new versions
- ✅ User data and settings are preserved
- ✅ The app maintains its identity across updates

## Current Status

The project is configured to:
1. **Use a release keystore** if configured in GitHub secrets (production mode)
2. **Fall back to debug keystore** if no release keystore is configured (development mode)

⚠️ **WARNING**: Debug-signed APKs cannot update each other because a new debug keystore is generated for each build. They are only suitable for testing, not distribution.

## One-Time Setup: Create Release Keystore

You only need to do this **once**. The same keystore will be used for all future releases.

### Step 1: Generate the Release Keystore

Run this command on your local machine:

```bash
keytool -genkey -v -keystore 4people-release.keystore \
  -alias 4people \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass YOUR_STRONG_PASSWORD \
  -keypass YOUR_STRONG_PASSWORD \
  -dname "CN=4people,OU=Emergency Communication,O=4people,L=City,ST=State,C=DE"
```

**Important**:
- Replace `YOUR_STRONG_PASSWORD` with a strong, unique password
- Store this password securely - you'll need it for GitHub secrets
- **Never commit the keystore file to git**
- Keep the keystore file safe - losing it means you can never update published APKs

### Step 2: Convert Keystore to Base64

GitHub secrets can only store text, so we need to encode the binary keystore file:

```bash
# On Linux/macOS
base64 -w 0 4people-release.keystore > 4people-release.keystore.base64

# On Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("4people-release.keystore")) > 4people-release.keystore.base64
```

This creates a text file containing the base64-encoded keystore.

### Step 3: Add Secrets to GitHub

1. Go to your GitHub repository
2. Navigate to **Settings → Secrets and variables → Actions**
3. Click **New repository secret** and add these four secrets:

| Secret Name | Value | Example |
|-------------|-------|---------|
| `RELEASE_KEYSTORE_BASE64` | Contents of `4people-release.keystore.base64` file | `MIIKBgIBAzCCCcAGCSqGSIb3DQ...` |
| `RELEASE_KEYSTORE_PASSWORD` | The password you used for the keystore | `YourStrongPassword123!` |
| `RELEASE_KEY_ALIAS` | The alias from the keytool command | `4people` |
| `RELEASE_KEY_PASSWORD` | The key password (same as keystore password) | `YourStrongPassword123!` |

### Step 4: Backup the Keystore

**CRITICAL**: Store the keystore file and passwords in a secure location:

- ✅ Use a password manager for the credentials
- ✅ Store the keystore file in encrypted backup storage
- ✅ Keep an offline copy in a safe location
- ❌ Never commit to git
- ❌ Never share publicly
- ❌ Never store in plain text

**If you lose the keystore, you can NEVER update APKs signed with it.**

## How It Works

### During GitHub Actions Build

1. The workflow checks if `RELEASE_KEYSTORE_BASE64` secret exists
2. If **yes** (production mode):
   - Decodes the base64 keystore to a file
   - Sets environment variables for Gradle
   - Gradle uses the release keystore to sign the APK
   - ✅ **APK is updateable**
3. If **no** (development mode):
   - Generates a fresh debug keystore
   - Gradle falls back to debug signing
   - ⚠️ **APK is NOT updateable** (warning printed in logs)

### During Local Build

When building locally with `./gradlew assembleRelease`:

1. Gradle checks for release keystore environment variables
2. If **set** (you configured them):
   - Uses your release keystore
   - ✅ **APK is updateable**
3. If **not set** (default):
   - Falls back to debug keystore
   - ⚠️ **APK is NOT updateable** (warning printed during build)

## Local Development with Release Keystore (Optional)

If you want to test release builds locally with the actual release keystore:

### Linux/macOS

```bash
# Set environment variables (in your shell or .bashrc/.zshrc)
export RELEASE_KEYSTORE_PATH="/path/to/4people-release.keystore"
export RELEASE_KEYSTORE_PASSWORD="YourStrongPassword123!"
export RELEASE_KEY_ALIAS="4people"
export RELEASE_KEY_PASSWORD="YourStrongPassword123!"

# Build release APK
./gradlew assembleRelease
```

### Windows (PowerShell)

```powershell
# Set environment variables
$env:RELEASE_KEYSTORE_PATH="C:\path\to\4people-release.keystore"
$env:RELEASE_KEYSTORE_PASSWORD="YourStrongPassword123!"
$env:RELEASE_KEY_ALIAS="4people"
$env:RELEASE_KEY_PASSWORD="YourStrongPassword123!"

# Build release APK
.\gradlew.bat assembleRelease
```

### Windows (Command Prompt)

```cmd
set RELEASE_KEYSTORE_PATH=C:\path\to\4people-release.keystore
set RELEASE_KEYSTORE_PASSWORD=YourStrongPassword123!
set RELEASE_KEY_ALIAS=4people
set RELEASE_KEY_PASSWORD=YourStrongPassword123!

gradlew.bat assembleRelease
```

## Verification

### Check if Release Keystore is Configured

After setting up GitHub secrets, trigger a release build and check the workflow logs:

**Success (updateable APKs)**:
```
Setting up release keystore from secrets...
✓ Release keystore configured - APKs will be updateable
```

**Failure (non-updateable APKs)**:
```
WARNING: No release keystore configured in GitHub secrets
Falling back to debug keystore - APKs will NOT be updateable
```

### Verify APK Signature

To verify an APK is signed with the correct keystore:

```bash
# Extract signing certificate
keytool -printcert -jarfile 4people-v1.0.0.apk

# Compare with your keystore certificate
keytool -list -v -keystore 4people-release.keystore -alias 4people -storepass YOUR_PASSWORD

# The certificate fingerprints (SHA256, SHA1, MD5) should match exactly
```

### Test Update Installation

1. Install an older release APK on an Android device
2. Try to install a newer release APK
3. Android should show "Update" instead of "Install"
4. After updating, verify the app data is preserved

If Android says "App not installed" or "Conflict with existing package", the APKs are signed with different keys.

## Troubleshooting

### "Keystore file does not exist" Error

**Cause**: `RELEASE_KEYSTORE_PATH` points to a non-existent file

**Solution**: 
- For GitHub Actions: Check that `RELEASE_KEYSTORE_BASE64` secret is set correctly
- For local builds: Verify the path in your environment variable is correct

### "Incorrect keystore password" Error

**Cause**: Wrong password in secrets or environment variables

**Solution**: Double-check the password matches what you used when creating the keystore

### APKs Still Not Updateable

**Possible causes**:
1. Different keystores used for different builds
2. GitHub secrets not configured
3. Wrong alias or password in secrets

**Debug steps**:
1. Check workflow logs for "Using release keystore" message
2. Verify certificate fingerprints match (see "Verify APK Signature" above)
3. Ensure all four GitHub secrets are set correctly

### Local Build Uses Debug Keystore

**Cause**: Environment variables not set

**Solution**: Set the four `RELEASE_*` environment variables before running Gradle

## Security Best Practices

1. **Never commit keystore files to git**
   - Add `*.keystore` to `.gitignore`
   - Add `*.jks` to `.gitignore`

2. **Use strong passwords**
   - Minimum 16 characters
   - Mix of uppercase, lowercase, numbers, symbols
   - Use a password manager

3. **Limit access to secrets**
   - Only repository admins should have access to GitHub secrets
   - Use GitHub's branch protection to prevent unauthorized releases

4. **Rotate keystore if compromised**
   - If keystore or password is leaked, generate a new keystore
   - Note: This will require users to uninstall and reinstall (data loss)
   - Communicate clearly with users about the security incident

5. **Backup the keystore**
   - Store encrypted backups in multiple secure locations
   - Test backup restoration periodically
   - Document the backup locations securely

## Google Play Store Publishing (Future)

If you plan to publish to Google Play Store:

1. **App Signing by Google Play** (Recommended):
   - Let Google Play manage the signing key
   - You upload an APK signed with an upload key
   - Google re-signs with their managed key
   - Google handles key security and rotation

2. **Manual Signing**:
   - Use the same release keystore for Google Play uploads
   - Never share the keystore file with anyone
   - Follow Google's security guidelines

For more information: https://developer.android.com/studio/publish/app-signing

## Summary

✅ **To enable updateable APKs:**
1. Generate a release keystore (one time)
2. Add four secrets to GitHub repository settings
3. Keep the keystore file and passwords secure
4. Never lose the keystore file

✅ **After setup:**
- All GitHub release builds will use the same keystore
- APKs will be updateable across versions
- Users can update without data loss

⚠️ **Without setup:**
- Builds fall back to debug keystore
- Each build uses a different key
- APKs are not updateable (for testing only)
