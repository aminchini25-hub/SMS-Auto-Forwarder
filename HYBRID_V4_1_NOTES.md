# SMS Auto Forwarder v4.1 Hybrid Safe

Base: multi-screen v4 redesign.

Merged improvements from the alternate redesign:
- HTTPS webhook validator exposed and unit-tested.
- Network errors do not log exception details that could accidentally expose endpoint/token context.
- Incoming sender is omitted from debug logs.
- Cleartext HTTP is disabled at the Android application level.
- Unused ACCESS_NETWORK_STATE permission removed.

UI structure:
- Home dashboard
- Rules
- Channels
- Settings
- Contact picker for both Sender and Recipient, without READ_CONTACTS permission.

Build-risk checks performed locally are documented in the comparison response.
