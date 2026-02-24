package com.intuit.playerui.xlr

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

object XlrSerializer {
    fun serialize(document: XlrDocument): String = serializeDocument(document).toString()

    fun serializeDocument(document: XlrDocument): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("object"))

            val objType = document.objectType
            put("properties", serializeProperties(objType.properties))
            objType.extends?.let { put("extends", serializeNode(it)) }
            putAdditionalItems("additionalProperties", objType.additionalProperties)
            putAnnotations(objType)

            // Document-level fields take precedence over objectType annotations
            put("source", JsonPrimitive(document.source))
            put("name", JsonPrimitive(document.name))
            document.genericTokens?.let { put("genericTokens", serializeParamTypeNodes(it)) }
        }

    fun serializeNode(node: NodeType): JsonObject =
        when (node) {
            is StringType -> serializeStringType(node)
            is NumberType -> serializeNumberType(node)
            is BooleanType -> serializeBooleanType(node)
            is NullType -> serializeNullType(node)
            is AnyType -> serializeAnyType(node)
            is UnknownType -> serializeUnknownType(node)
            is UndefinedType -> serializeUndefinedType(node)
            is VoidType -> serializeVoidType(node)
            is NeverType -> serializeNeverType(node)
            is RefType -> serializeRefType(node)
            is ObjectType -> serializeObjectType(node)
            is ArrayType -> serializeArrayType(node)
            is TupleType -> serializeTupleType(node)
            is RecordType -> serializeRecordType(node)
            is OrType -> serializeOrType(node)
            is AndType -> serializeAndType(node)
            is TemplateLiteralType -> serializeTemplateLiteralType(node)
            is ConditionalType -> serializeConditionalType(node)
            is FunctionType -> serializeFunctionType(node)
        }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putAnnotations(node: NodeType) {
        node.source?.let { put("source", JsonPrimitive(it)) }
        node.name?.let { put("name", JsonPrimitive(it)) }
        node.title?.let { put("title", JsonPrimitive(it)) }
        node.description?.let { put("description", JsonPrimitive(it)) }
        node.examples?.let { put("examples", it) }
        node.default?.let { put("default", it) }
        node.see?.let { put("see", it) }
        node.comment?.let { put("comment", JsonPrimitive(it)) }
        node.meta?.let { metaMap ->
            put(
                "meta",
                buildJsonObject {
                    metaMap.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
                },
            )
        }
        node.genericTokens?.let { put("genericTokens", serializeParamTypeNodes(it)) }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putAdditionalItems(
        key: String,
        items: AdditionalItemsType,
    ) {
        when (items) {
            is AdditionalItemsType.None -> put(key, JsonPrimitive(false))
            is AdditionalItemsType.Typed -> put(key, serializeNode(items.node))
        }
    }

    private fun serializeStringType(node: StringType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("string"))
            node.const?.let { put("const", JsonPrimitive(it)) }
            node.enum?.let { put("enum", buildJsonArray { it.forEach { v -> add(JsonPrimitive(v)) } }) }
            putAnnotations(node)
        }

    private fun serializeNumberType(node: NumberType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("number"))
            node.const?.let { put("const", doubleToJsonPrimitive(it)) }
            node.enum?.let { put("enum", buildJsonArray { it.forEach { v -> add(doubleToJsonPrimitive(v)) } }) }
            putAnnotations(node)
        }

    private fun serializeBooleanType(node: BooleanType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("boolean"))
            node.const?.let { put("const", JsonPrimitive(it)) }
            putAnnotations(node)
        }

    private fun serializeNullType(node: NullType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("null"))
            putAnnotations(node)
        }

    private fun serializeAnyType(node: AnyType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("any"))
            putAnnotations(node)
        }

    private fun serializeUnknownType(node: UnknownType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("unknown"))
            putAnnotations(node)
        }

    private fun serializeUndefinedType(node: UndefinedType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("undefined"))
            putAnnotations(node)
        }

    private fun serializeVoidType(node: VoidType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("void"))
            putAnnotations(node)
        }

    private fun serializeNeverType(node: NeverType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("never"))
            putAnnotations(node)
        }

    private fun serializeRefType(node: RefType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("ref"))
            put("ref", JsonPrimitive(node.ref))
            node.genericArguments?.let {
                put("genericArguments", buildJsonArray { it.forEach { n -> add(serializeNode(n)) } })
            }
            node.property?.let { put("property", JsonPrimitive(it)) }
            putAnnotations(node)
        }

    private fun serializeObjectType(node: ObjectType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", serializeProperties(node.properties))
            node.extends?.let { put("extends", serializeNode(it)) }
            putAdditionalItems("additionalProperties", node.additionalProperties)
            putAnnotations(node)
        }

    private fun serializeArrayType(node: ArrayType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("array"))
            put("elementType", serializeNode(node.elementType))
            putAnnotations(node)
        }

    private fun serializeTupleType(node: TupleType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("tuple"))
            put("elementTypes", buildJsonArray { node.elementTypes.forEach { add(serializeTupleMember(it)) } })
            put("minItems", JsonPrimitive(node.minItems))
            putAdditionalItems("additionalItems", node.additionalItems)
            putAnnotations(node)
        }

    private fun serializeRecordType(node: RecordType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("record"))
            put("keyType", serializeNode(node.keyType))
            put("valueType", serializeNode(node.valueType))
            putAnnotations(node)
        }

    private fun serializeOrType(node: OrType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("or"))
            put("or", buildJsonArray { node.orTypes.forEach { add(serializeNode(it)) } })
            putAnnotations(node)
        }

    private fun serializeAndType(node: AndType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("and"))
            put("and", buildJsonArray { node.andTypes.forEach { add(serializeNode(it)) } })
            putAnnotations(node)
        }

    private fun serializeTemplateLiteralType(node: TemplateLiteralType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("template"))
            put("format", JsonPrimitive(node.format))
            putAnnotations(node)
        }

    private fun serializeConditionalType(node: ConditionalType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("conditional"))
            put(
                "check",
                buildJsonObject {
                    put("left", serializeNode(node.check.left))
                    put("right", serializeNode(node.check.right))
                },
            )
            put(
                "value",
                buildJsonObject {
                    put("true", serializeNode(node.value.trueValue))
                    put("false", serializeNode(node.value.falseValue))
                },
            )
            putAnnotations(node)
        }

    private fun serializeFunctionType(node: FunctionType): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("function"))
            put(
                "parameters",
                buildJsonArray {
                    node.parameters.forEach { add(serializeFunctionParameter(it)) }
                },
            )
            node.returnType?.let { put("returnType", serializeNode(it)) }
            putAnnotations(node)
        }

    private fun serializeFunctionParameter(param: FunctionParameter): JsonObject =
        buildJsonObject {
            put("name", JsonPrimitive(param.name))
            put("type", serializeNode(param.type))
            param.optional?.let { put("optional", JsonPrimitive(it)) }
            param.default?.let { put("default", serializeNode(it)) }
        }

    private fun serializeProperties(properties: Map<String, ObjectProperty>): JsonObject =
        buildJsonObject {
            properties.forEach { (key, prop) ->
                put(key, serializeObjectProperty(prop))
            }
        }

    private fun serializeObjectProperty(prop: ObjectProperty): JsonObject =
        buildJsonObject {
            put("required", JsonPrimitive(prop.required))
            put("node", serializeNode(prop.node))
        }

    private fun serializeTupleMember(member: TupleMember): JsonObject =
        buildJsonObject {
            member.name?.let { put("name", JsonPrimitive(it)) }
            put("type", serializeNode(member.type))
            member.optional?.let { put("optional", JsonPrimitive(it)) }
        }

    private fun doubleToJsonPrimitive(value: Double): JsonPrimitive =
        if (value.isFinite() && value % 1.0 == 0.0) {
            JsonPrimitive(value.toLong())
        } else {
            JsonPrimitive(value)
        }

    private fun serializeParamTypeNodes(tokens: List<ParamTypeNode>): JsonArray =
        buildJsonArray {
            tokens.forEach { token ->
                add(
                    buildJsonObject {
                        put("symbol", JsonPrimitive(token.symbol))
                        token.constraints?.let { put("constraints", serializeNode(it)) }
                        token.default?.let { put("default", serializeNode(it)) }
                    },
                )
            }
        }
}
