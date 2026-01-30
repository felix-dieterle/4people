# Google Assistant Integration and App Shortcuts

This document explains how to use Google Assistant and App Shortcuts to quickly activate Panic Mode and Emergency Communication Mode in the 4people app.

## Features

The 4people app now supports two ways to quickly activate emergency modes:

1. **App Shortcuts** - Long-press the app icon
2. **Google Assistant Integration** - Voice commands

## Using App Shortcuts

### Activating via App Icon

1. **Long-press** the 4people app icon on your home screen or app drawer
2. You'll see two shortcuts:
   - **Panic Mode** - Activates panic monitoring
   - **Emergency** - Activates emergency communication mode
3. Tap the desired shortcut to immediately activate that mode

### Adding Shortcuts to Home Screen (Android 8.0+)

1. Long-press the app icon
2. Drag one of the shortcuts to your home screen
3. The shortcut will remain on your home screen for quick access

## Using Google Assistant

You can use voice commands to activate either mode through Google Assistant:

### Voice Commands

- **"Ok Google, activate Panic Mode in 4people"**
  - Launches the app and activates Panic Mode
  - Switches to the Panic tab automatically
  
- **"Ok Google, activate Emergency in 4people"**
  - Launches the app and activates Emergency Communication Mode
  - Switches to the Emergency tab automatically

### Setup (First Time Only)

The app shortcuts are automatically configured when you install the app. No manual setup is required!

## How It Works

### Panic Mode Activation

When you activate Panic Mode via shortcut or Google Assistant:

1. The app opens (if not already open)
2. Switches to the Panic tab
3. If Panic Mode is not active, shows the activation confirmation dialog
4. Once confirmed, Panic Mode begins monitoring

If Panic Mode is already active, you'll see a toast notification confirming it's running.

### Emergency Mode Activation

When you activate Emergency Mode via shortcut or Google Assistant:

1. The app opens (if not already open)
2. Switches to the Emergency tab
3. If permissions are granted and Emergency Mode is not active, it activates immediately
4. If permissions are needed, shows the permission request dialog

If Emergency Mode is already active, you'll see a toast notification confirming it's running.

## Benefits

- **Quick Access**: No need to open the app and navigate to the right tab
- **Hands-Free**: Use voice commands in emergency situations
- **One-Tap Activation**: Add shortcuts to your home screen for instant access
- **Always Available**: Shortcuts work even when the app is closed

## Technical Details

### Intent Actions

The implementation uses the following custom intent actions:

- `com.fourpeople.adhoc.action.ACTIVATE_PANIC_MODE`
- `com.fourpeople.adhoc.action.ACTIVATE_EMERGENCY_MODE`

These intents are handled by the MainActivity and automatically switch to the appropriate tab and activate the selected mode.

### Automatic Configuration

The shortcuts are defined in the app's resources (`shortcuts.xml`) and are automatically registered when the app is installed. This means:

- No manual configuration required
- Works immediately after installation
- Shortcuts update automatically with app updates

## Troubleshooting

### Shortcuts Don't Appear

- **Check Android Version**: App shortcuts require Android 7.1 (API 25) or higher
- **Restart Launcher**: Try restarting your device or launcher app
- **Reinstall**: Uninstall and reinstall the app if shortcuts don't appear

### Google Assistant Doesn't Recognize Commands

- **Check Permissions**: Ensure Google Assistant has permission to access app actions
- **Exact Phrasing**: Try using the exact phrases listed above
- **Alternative**: Try saying "Open 4people and activate [mode]"
- **App Visibility**: Ensure the app is not hidden from Google Assistant

### Activation Not Working

- **Permissions**: Ensure all required permissions are granted
- **Check Status**: The app won't re-activate if the mode is already running
- **Logs**: Check the system log in the app for any error messages

## Privacy and Security

- **Local Only**: All shortcuts and intent handling happen locally on your device
- **No Data Sharing**: Google Assistant integration doesn't share any data with Google
- **Secure**: Intents require the app to be installed and cannot be triggered by malicious apps

## Future Enhancements

Potential future improvements:

- Custom voice commands
- Shortcut to specific emergency contacts
- Quick status check via Google Assistant
- Widget integration with shortcuts

## Feedback

If you have suggestions for improving the shortcuts or Google Assistant integration, please file an issue on the project's GitHub repository.
