package com.example.smsautoforwarder

import android.telephony.SmsManager

/**
 * Translates the low-level failure codes surfaced by [SmsStatusReceiver] and
 * [NetworkForwarder] into a string resource the UI can display, instead of a
 * raw integer or a generic "failed" label.
 */
object ErrorReasons {

    fun smsResultLabel(resultCode: Int): Int = when (resultCode) {
        PrefsHelper.ERROR_CODE_PRE_SEND -> R.string.sms_error_pre_send
        SmsManager.RESULT_ERROR_NO_SERVICE -> R.string.sms_error_no_service
        SmsManager.RESULT_ERROR_RADIO_OFF -> R.string.sms_error_radio_off
        SmsManager.RESULT_ERROR_NULL_PDU -> R.string.sms_error_null_pdu
        SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> R.string.sms_error_limit_exceeded
        SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE -> R.string.sms_error_fdn_blocked
        SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED -> R.string.sms_error_short_code_not_allowed
        SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED -> R.string.sms_error_short_code_never_allowed
        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> R.string.sms_error_generic
        else -> R.string.sms_error_generic
    }

    fun networkResultLabel(status: ForwardStatus): Int = when (status) {
        ForwardStatus.SUCCESS -> R.string.last_event_sent
        ForwardStatus.HTTP_ERROR -> R.string.network_error_http
        ForwardStatus.TIMEOUT -> R.string.network_error_timeout
        ForwardStatus.NO_CONNECTION -> R.string.network_error_no_connection
        ForwardStatus.INVALID_CONFIG -> R.string.network_error_invalid_config
        ForwardStatus.UNKNOWN_ERROR -> R.string.network_error_unknown
    }
}
