package com.chathuninimesha.coreflow.data.local

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import java.lang.reflect.Type

sealed class LoadedList<T> {
    data class Data<T>(val items: List<T>) : LoadedList<T>()
    class Corrupt<T> : LoadedList<T>()
}

class JsonListParser(private val gson: Gson) {

    fun <T> readList(
        json: String?,
        type: Type,
        sanitize: (T) -> T?
    ): LoadedList<T> {
        if (json.isNullOrBlank()) {
            return LoadedList.Data(emptyList())
        }
        val parsed: List<T?> = try {
            gson.fromJson<List<T>>(json, type) ?: return LoadedList.Data(emptyList())
        } catch (_: JsonSyntaxException) {
            return LoadedList.Corrupt()
        } catch (_: JsonParseException) {
            return LoadedList.Corrupt()
        } catch (_: Exception) {
            return LoadedList.Corrupt()
        }
        val valid = parsed.mapNotNull { item -> item?.let(sanitize) }
        if (parsed.isNotEmpty() && valid.isEmpty()) {
            return LoadedList.Corrupt()
        }
        return LoadedList.Data(valid)
    }

    fun <T> writeList(items: List<T>): String = gson.toJson(items)
}
