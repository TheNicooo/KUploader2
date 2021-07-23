package com.kauel.kuploader2.api.login

data class Token(
    val client_id: Int? = null,
    val created_at: String? = null,
    val expires_at: String? = null,
    val id: String? = null,
    val name: String? = null,
    val revoked: Boolean? = null,
    val updated_at: String? = null,
    val user_id: Int? = null
)