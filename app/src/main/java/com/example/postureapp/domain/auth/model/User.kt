package com.example.postureapp.domain.auth.model

data class User(
    val id: String,
    val email: String,
    val isActive: Boolean,
    val isSuperuser: Boolean
)

