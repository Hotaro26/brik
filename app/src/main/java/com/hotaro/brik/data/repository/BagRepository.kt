package com.hotaro.brik.data.repository

import android.content.Context
import com.hotaro.brik.data.model.AppBag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class BagRepository(context: Context) {
    private val prefs = context.getSharedPreferences("brik_bags_pref", Context.MODE_PRIVATE)
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val _bags = MutableStateFlow<List<AppBag>>(emptyList())

        @Volatile
        private var isLoaded = false
    }

    val bags: StateFlow<List<AppBag>> = _bags.asStateFlow()

    init {
        if (!isLoaded) {
            synchronized(BagRepository::class.java) {
                if (!isLoaded) {
                    loadBags()
                    isLoaded = true
                }
            }
        }
    }

    private fun loadBags() {
        val serialized = prefs.getString("saved_bags", null)
        if (!serialized.isNullOrBlank()) {
            try {
                val list = json.decodeFromString<List<AppBag>>(serialized)
                _bags.value = list
            } catch (e: Exception) {
                e.printStackTrace()
                _bags.value = emptyList()
            }
        } else {
            _bags.value = emptyList()
        }
    }

    fun saveBag(name: String, packageNames: Set<String>): AppBag {
        val newBag = AppBag(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            packageNames = packageNames
        )
        val updated = _bags.value + newBag
        persistBags(updated)
        return newBag
    }

    fun updateBag(bag: AppBag): AppBag {
        val existingIndex = _bags.value.indexOfFirst { it.id == bag.id }
        val updated = if (existingIndex >= 0) {
            _bags.value.toMutableList().apply { set(existingIndex, bag) }
        } else {
            _bags.value + bag
        }
        persistBags(updated)
        return bag
    }

    fun deleteBag(bagId: String) {
        val updated = _bags.value.filterNot { it.id == bagId }
        persistBags(updated)
    }

    private fun persistBags(newList: List<AppBag>) {
        _bags.value = newList
        val serialized = json.encodeToString(newList)
        prefs.edit().putString("saved_bags", serialized).apply()
    }
}
