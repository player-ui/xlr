package com.intuit.playerui.xlr

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

object XlrSerializer {
    fun serialize(document: XlrDocument): String = serializeDocument(document).toString()

    // Wire format requires ObjectType fields flattened at the document root level
    // (not nested under an "objectType" key), so we manually merge the node JSON
    // with document-level name/source/genericTokens which take precedence.
    fun serializeDocument(document: XlrDocument): JsonObject =
        buildJsonObject {
            val nodeJson = xlrJson.encodeToJsonElement(NodeType.serializer(), document.objectType).jsonObject
            nodeJson.forEach { (key, value) -> put(key, value) }
            put("source", JsonPrimitive(document.source))
            put("name", JsonPrimitive(document.name))
            document.genericTokens?.let {
                put("genericTokens", xlrJson.encodeToJsonElement(it))
            }
        }

    fun serializeNode(node: NodeType): JsonObject = xlrJson.encodeToJsonElement(NodeType.serializer(), node).jsonObject
}
