package com.example.postureapp.core.utils

import android.util.Patterns

object InputValidators {
    fun validateEmail(value: String): String? {
        if (value.isBlank()) return "Email is required"
        return if (Patterns.EMAIL_ADDRESS.matcher(value).matches()) null else "Enter a valid email"
    }

    fun validatePassword(value: String, minLength: Int = 8): String? {
        if (value.isBlank()) return "Password is required"
        return if (value.length >= minLength) null else "Use at least $minLength characters"
    }
}

