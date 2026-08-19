# SMS Auto Forwarder

A small offline Android app that automatically forwards incoming SMS from one configured sender to one configured recipient.

Key points:

- Kotlin Android app
- Android 6.0+
- No READ_SMS permission
- Explicit runtime RECEIVE_SMS / SEND_SMS permissions
- INTERNET permission used only if Telegram or Webhook forwarding is enabled
- Phone-number normalization including Persian/Arabic digits
- Alphanumeric sender-ID support
- Multipart SMS support
- Sent / failure / delivery status callbacks
- Keyword include/exclude filter on the message body
- Independent forwarding channels: SMS, Telegram bot, generic Webhook (JSON POST)
- English + Persian resources
- Samsung and Xiaomi background guidance
- GitHub Actions APK build workflow

See `README_FA.md` for the full Persian guide.
