package com.chathuninimesha.coreflow.auth

import com.chathuninimesha.coreflow.R

fun AuthError.toMessageRes(): Int = when (this) {
    AuthError.NAME_REQUIRED -> R.string.error_name_required
    AuthError.EMAIL_REQUIRED -> R.string.error_email_required
    AuthError.EMAIL_INVALID -> R.string.error_email_invalid
    AuthError.PASSWORD_REQUIRED -> R.string.error_password_required
    AuthError.PASSWORD_TOO_SHORT -> R.string.error_password_too_short
    AuthError.CONFIRM_PASSWORD_REQUIRED -> R.string.error_confirm_password_required
    AuthError.PASSWORD_MISMATCH -> R.string.error_password_mismatch
    AuthError.DUPLICATE_EMAIL -> R.string.error_duplicate_email
    AuthError.PROFILE_EXISTS -> R.string.error_profile_exists
    AuthError.UNKNOWN_EMAIL -> R.string.error_unknown_email
    AuthError.WRONG_PASSWORD -> R.string.error_wrong_password
    AuthError.INVALID_CREDENTIALS -> R.string.invalid_email_or_password
    AuthError.PROFILE_CORRUPT -> R.string.error_profile_corrupt
    AuthError.STORAGE_ERROR -> R.string.error_storage
}
