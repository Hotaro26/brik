package com.hotaro.brik.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppBag(
    val id: String,
    val name: String,
    val packageNames: Set<String>,
    val createdAt: Long = System.currentTimeMillis()
)
