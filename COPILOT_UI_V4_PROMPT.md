# Copilot Agent prompt — UI v4 redesign

Work directly inside this repository and redesign the Android app UI. Do not only explain changes; edit files, run checks, commit/push, monitor GitHub Actions, and keep fixing until the APK artifact builds successfully.

## Product goal
Turn SMS Auto Forwarder from a single crowded settings form into a polished, modern, easy-to-use app while preserving all forwarding functionality.

## Required UX structure
Use four clear screens/tabs with bottom navigation:
1. Home — large Active/Paused status card, enable switch, sender/recipient/channel summary, last forwarding result, permission status, quick links to edit rule/channels.
2. Rules — sender, recipient, Contacts picker buttons, keyword include/exclude filter, Save Rule.
3. Channels — separate visual cards for SMS, Telegram, and HTTPS Webhook; show configuration fields only when each channel is enabled; Save Channels.
4. Settings — contextual SMS permissions, device-specific background/battery guidance, app settings shortcut, and an explanation of Android sideload/Play Protect warnings.

## Visual design
- Modern Material look with generous spacing and rounded cards.
- Strong but professional color palette, not a plain gray form.
- A colorful hero/status card on Home.
- Different subtle background accents for SMS, Telegram, and Webhook cards.
- Proper light/dark mode.
- Persian RTL and English strings must stay complete and in parity.
- Avoid custom dotted style names that accidentally imply an Android style parent. If a dotted custom style is unavoidable, always specify an explicit valid parent.

## Contacts integration
Add system contact selection for BOTH sender and recipient.
Use Intent.ACTION_PICK with ContactsContract.CommonDataKinds.Phone.CONTENT_URI (or the modern system Contacts Picker where supported).
Do NOT add broad READ_CONTACTS permission just to pick one phone number.
Persist the selected number immediately into the form and then SharedPreferences when the user saves.
The sender field must still allow manual alphanumeric sender IDs such as BANK.

## Permissions UX
Do not request SMS permissions on app launch.
Request them contextually only when the user enables forwarding or explicitly taps the permissions button.
RECEIVE_SMS is required for automatic forwarding.
SEND_SMS is required only when the SMS forwarding channel is enabled.
If only Telegram/Webhook are enabled, do not report SEND_SMS as missing.
Explain what is being requested before opening the Android permission dialog.

## Install warning
Do not attempt to bypass Android unknown-source or Play Protect warnings. Explain in Settings that a GitHub/sideloaded APK is outside Google Play and Android controls that security confirmation.

## Preserve functionality
Do not break or remove:
- incoming SMS receiver
- sender matching including Iranian number normalization, Persian/Arabic digits, and textual sender IDs
- multipart SMS handling
- SMS forwarding and sent/delivery status
- Telegram forwarding
- keyword include/exclude filtering
- HTTPS Webhook forwarding
- Persian and English UI
- Samsung/Xiaomi background guidance

## Security and validation
- Webhook must be HTTPS-only.
- Never log SMS body, Telegram bot token, or credentials.
- Do not allow forwarding to be enabled with no channel selected.
- SMS channel requires a valid recipient.
- Telegram channel requires bot token and chat ID.

## Build quality
Scan all XML resources and style references for AAPT/linking issues.
Specifically prevent failures like:
- invalid string escape/unicode sequences
- resource style/Widget not found
- missing R.string/R.id/R.color/R.drawable/R.layout references

Run:
- git diff --check
- gradle testDebugUnitTest --stacktrace
- gradle assembleDebug --stacktrace

After pushing, monitor the new GitHub Actions run. If it fails, read the failed job log, fix the next root cause, push again, and repeat until all steps succeed.

Final success means:
- Run unit tests = success
- Build installable debug APK = success
- Upload APK = success
- artifact SMS-Auto-Forwarder-APK exists
- artifact contains app-debug.apk

Only report completion after the workflow is green and the artifact is verified.
