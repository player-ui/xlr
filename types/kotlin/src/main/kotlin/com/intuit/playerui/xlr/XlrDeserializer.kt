package com.intuit.playerui.xlr

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object XlrDeserializer {
    fun deserialize(jsonString: String): XlrDocument {
        val element = xlrJson.parseToJsonElement(jsonString).jsonObject
        return parseDocument(element)
    }

    fun parseDocument(obj: JsonObject): XlrDocument {
        val node = xlrJson.decodeFromJsonElement(NodeType.serializer(), obj)
        val objectType =
            node as? ObjectType
                ?: throw IllegalArgumentException(
                    "Document root must be an object type, got: ${obj["type"]}",
                )
        return XlrDocument(
            name =
                obj["name"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Document missing 'name'"),
            source =
                obj["source"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Document missing 'source'"),
            objectType = objectType,
            genericTokens =
                obj["genericTokens"]?.takeIf { it !is JsonNull }?.let {
                    xlrJson.decodeFromJsonElement(it)
                },
        )
    }

    fun parseNode(element: JsonElement): NodeType {
        if (element is JsonNull) return NullType()
        return xlrJson.decodeFromJsonElement(NodeType.serializer(), element)
    }
}
