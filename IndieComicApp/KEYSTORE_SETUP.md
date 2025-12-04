# Keystore Setup Guide

This guide will help you set up secure keystore management for your Android app.

## Quick Start

### Step 1: Generate the Keystore

**On Windows (PowerShell):**
```powershell
.\keystore\generate-keystore.ps1
```

**On Linux/Mac:**
```bash
chmod +x keystore/generate-keystore.sh
./keystore/generate-keystore.sh
```

**Or manually:**
```bash
keytool -genkey -v -keystore keystore/release.keystore -alias indiecomic-key -keyalg RSA -keysize 2048 -validity 10000
```

You'll be prompted for:
- **Keystore password**: Choose a strong, unique password
- **Key password**: Can be the same or different
- **Your details**: Name, organization, city, etc.

### Step 2: Create keystore.properties

1. Copy the template:
   ```bash
   cp keystore.properties.template keystore.properties
   ```

2. Edit `keystore.properties` and fill in your actual passwords:
   ```properties
   storePassword=your_actual_keystore_password
   keyPassword=your_actual_key_password
   keyAlias=indiecomic-key
   storeFile=keystore/release.keystore
   ```

indiecomic-password

### Step 3: Get SHA-1 Fingerprint for Firebase

Run:
```bash
.\gradlew.bat signingReport
```

Look for the **SHA1** value under the `release` variant and add it to Firebase Console.

### Step 4: Test the Build

Build a release APK to verify everything works:
```bash
.\gradlew.bat assembleRelease
```

The signed APK will be at: `app/build/outputs/apk/release/app-release.apk`

## Security Best Practices

### Local Storage

1. **Password Manager**: Store keystore passwords in a password manager (1Password, Bitwarden, etc.)
2. **Encrypted Backup**: Back up the keystore file to encrypted cloud storage or secure backup drive
3. **Multiple Backups**: Keep backups in multiple secure locations
4. **Documentation**: Document where backups are stored (but not the passwords themselves)

### File Locations

- **Keystore file**: `keystore/release.keystore` (gitignored)
- **Properties file**: `keystore.properties` (gitignored)
- **Templates**: Safe to commit (no sensitive data)

### What's Gitignored

The following are automatically ignored:
- `*.jks`, `*.keystore` files
- `keystore.properties`
- `keystore/` directory contents

## For CI/CD (Future)

When you're ready to set up CI/CD:

1. See `keystore/ci-cd-template.md` for detailed instructions
2. Encode keystore as base64 for GitHub Secrets
3. Store passwords as GitHub Secrets
4. Use the provided GitHub Actions workflow template

## Troubleshooting

### "Keystore file not found"
- Make sure `keystore.properties` exists and `storeFile` path is correct
- Path should be relative to project root: `keystore/release.keystore`

### "Wrong password"
- Double-check passwords in `keystore.properties`
- Make sure there are no extra spaces or quotes

### "Signing config not found"
- The build will work without keystore for debug builds
- Release builds require `keystore.properties` to be configured

## Important Notes

⚠️ **CRITICAL**: If you lose your keystore or forget the password:
- You **cannot** update your app on Google Play Store
- You'll need to create a new app listing
- All existing users will need to uninstall and reinstall

**Always keep secure backups!**

