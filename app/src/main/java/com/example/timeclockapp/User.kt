package com.example.timeclockapp

data class User(
    val username: String,
    val password: String,
    val preferredFirstName: String,
    val isAdmin: Boolean
)