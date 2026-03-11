package com.vide.model

enum class ContactActionType {
    CALL, SMS, WHATSAPP, EMAIL
}

data class ContactAction(
    val contactName: String,
    val actionLabel: String,
    val actionType: ContactActionType,
    val data: String,
    val key: String
)
