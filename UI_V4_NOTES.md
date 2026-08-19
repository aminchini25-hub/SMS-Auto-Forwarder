# UI v4 redesign

- Four-screen bottom navigation: Home, Rules, Channels, Settings.
- Contact picker for sender and recipient using ACTION_PICK + Phone.CONTENT_URI; no READ_CONTACTS permission is added.
- Permission prompts are contextual: RECEIVE_SMS is needed for forwarding, SEND_SMS only when the SMS channel is enabled.
- GitHub/sideload install warning is explained in Settings; Android unknown-source confirmation cannot be bypassed by the app.
- Webhook validation and NetworkForwarder are HTTPS-only.
- New colorful dashboard, channel cards, dark mode palette, and clearer setup states.
- Existing SMS/Telegram/Webhook/filter forwarding logic is preserved.
