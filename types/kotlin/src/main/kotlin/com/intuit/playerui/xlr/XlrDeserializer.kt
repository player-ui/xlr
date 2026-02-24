package com.intuit.playerui.xlr

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object XlrDeserializer {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun deserialize(jsonString: String): XlrDocument {
        val element = json.parseToJsonElement(jsonString).jsonObject
        return parseDocument(element)
    }

    fun parseDocument(obj: JsonObject): XlrDocument {
        val annotations = parseAnnotations(obj)
        val objectType =
            ObjectType(
                properties = parseProperties(obj["properties"]),
                extends = obj["extends"]?.let { parseRefType(it.jsonObject) },
                additionalProperties = parseAdditionalItems(obj["additionalProperties"]),
                name = annotations.name,
                title = annotations.title,
                description = annotations.description,
                examples = annotations.examples,
                default = annotations.default,
                see = annotations.see,
                comment = annotations.comment,
                meta = annotations.meta,
                source = annotations.source,
                genericTokens = annotations.genericTokens,
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
                obj["genericTokens"]?.takeIf { it !is JsonNull }?.jsonArray?.map {
                    parseParamTypeNode(it.jsonObject)
                },
        )
    }

    fun parseNode(element: JsonElement): NodeType {
        if (element is JsonNull) {
            return NullType()
        }

        val obj = element.jsonObject
        val type =
            obj["type"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Node missing 'type' field: $obj")

        return when (type) {
            "string" -> parseStringType(obj)
            "number" -> parseNumberType(obj)
            "boolean" -> parseBooleanType(obj)
            "null" -> parseNullType(obj)
            "any" -> parseAnyType(obj)
            "unknown" -> parseUnknownType(obj)
            "undefined" -> parseUndefinedType(obj)
            "void" -> parseVoidType(obj)
            "never" -> parseNeverType(obj)
            "ref" -> parseRefType(obj)
            "object" -> parseObjectType(obj)
            "array" -> parseArrayType(obj)
            "tuple" -> parseTupleType(obj)
            "record" -> parseRecordType(obj)
            "or" -> parseOrType(obj)
            "and" -> parseAndType(obj)
            "template" -> parseTemplateLiteralType(obj)
            "conditional" -> parseConditionalType(obj)
            "function" -> parseFunctionType(obj)
            else -> throw IllegalArgumentException("Unknown type: $type in object: $obj")
        }
    }

    private data class ParsedAnnotations(
        val name: String? = null,
        val title: String? = null,
        val description: String? = null,
        val examples: JsonElement? = null,
        val default: JsonElement? = null,
        val see: JsonElement? = null,
        val comment: String? = null,
        val meta: Map<String, String>? = null,
        val source: String? = null,
        val genericTokens: List<ParamTypeNode>? = null,
    )

    private fun parseAnnotations(obj: JsonObject): ParsedAnnotations =
        ParsedAnnotations(
            name = obj["name"]?.jsonPrimitive?.contentOrNull,
            title = obj["title"]?.jsonPrimitive?.contentOrNull,
            description = obj["description"]?.jsonPrimitive?.contentOrNull,
            examples = obj["examples"]?.takeIf { it !is JsonNull },
            default = obj["default"]?.takeIf { it !is JsonNull },
            see = obj["see"]?.takeIf { it !is JsonNull },
            comment = obj["comment"]?.jsonPrimitive?.contentOrNull,
            meta =
                obj["meta"]?.takeIf { it !is JsonNull }?.jsonObject?.mapValues {
                    it.value.jsonPrimitive.content
                },
            source = obj["source"]?.jsonPrimitive?.contentOrNull,
            genericTokens =
                obj["genericTokens"]?.takeIf { it !is JsonNull }?.jsonArray?.map {
                    parseParamTypeNode(it.jsonObject)
                },
        )

    private fun parseAdditionalItems(element: JsonElement?): AdditionalItemsType {
        if (element == null || element is JsonNull) return AdditionalItemsType.None
        if (element is JsonPrimitive && element.booleanOrNull == false) return AdditionalItemsType.None
        return AdditionalItemsType.Typed(parseNode(element))
    }

    private fun parseStringType(obj: JsonObject): StringType {
        val a = parseAnnotations(obj)
        return StringType(
            const = obj["const"]?.jsonPrimitive?.contentOrNull,
            enum = obj["enum"]?.jsonArray?.map { it.jsonPrimitive.content },
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseNumberType(obj: JsonObject): NumberType {
        val a = parseAnnotations(obj)
        return NumberType(
            const = obj["const"]?.jsonPrimitive?.doubleOrNull,
            enum =
                obj["enum"]?.jsonArray?.map {
                    it.jsonPrimitive.doubleOrNull
                        ?: throw IllegalArgumentException("Invalid number in enum: ${it.jsonPrimitive.content}")
                },
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseBooleanType(obj: JsonObject): BooleanType {
        val a = parseAnnotations(obj)
        return BooleanType(
            const = obj["const"]?.jsonPrimitive?.booleanOrNull,
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseNullType(obj: JsonObject): NullType {
        val a = parseAnnotations(obj)
        return NullType(
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseAnyType(obj: JsonObject): AnyType {
        val a = parseAnnotations(obj)
        return AnyType(
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseUnknownType(obj: JsonObject): UnknownType {
        val a = parseAnnotations(obj)
        return UnknownType(
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseUndefinedType(obj: JsonObject): UndefinedType {
        val a = parseAnnotations(obj)
        return UndefinedType(
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseVoidType(obj: JsonObject): VoidType {
        val a = parseAnnotations(obj)
        return VoidType(
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseNeverType(obj: JsonObject): NeverType {
        val a = parseAnnotations(obj)
        return NeverType(
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseRefType(obj: JsonObject): RefType {
        val a = parseAnnotations(obj)
        return RefType(
            ref =
                obj["ref"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("RefType missing 'ref': $obj"),
            genericArguments = obj["genericArguments"]?.jsonArray?.map { parseNode(it) },
            property = obj["property"]?.jsonPrimitive?.contentOrNull,
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseObjectType(obj: JsonObject): ObjectType {
        val a = parseAnnotations(obj)
        return ObjectType(
            properties = parseProperties(obj["properties"]),
            extends = obj["extends"]?.let { parseRefType(it.jsonObject) },
            additionalProperties = parseAdditionalItems(obj["additionalProperties"]),
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseArrayType(obj: JsonObject): ArrayType {
        val elementType =
            obj["elementType"]
                ?: throw IllegalArgumentException("Array missing 'elementType': $obj")
        val a = parseAnnotations(obj)
        return ArrayType(
            elementType = parseNode(elementType),
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseTupleType(obj: JsonObject): TupleType {
        val a = parseAnnotations(obj)
        return TupleType(
            elementTypes =
                obj["elementTypes"]?.jsonArray?.map { parseTupleMember(it.jsonObject) }
                    ?: emptyList(),
            minItems = obj["minItems"]?.jsonPrimitive?.int ?: 0,
            additionalItems = parseAdditionalItems(obj["additionalItems"]),
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseTupleMember(obj: JsonObject): TupleMember =
        TupleMember(
            name = obj["name"]?.jsonPrimitive?.contentOrNull,
            type = parseNode(obj["type"] ?: throw IllegalArgumentException("TupleMember missing 'type'")),
            optional = obj["optional"]?.jsonPrimitive?.booleanOrNull,
        )

    private fun parseRecordType(obj: JsonObject): RecordType {
        val a = parseAnnotations(obj)
        return RecordType(
            keyType = parseNode(obj["keyType"] ?: throw IllegalArgumentException("Record missing 'keyType'")),
            valueType = parseNode(obj["valueType"] ?: throw IllegalArgumentException("Record missing 'valueType'")),
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseOrType(obj: JsonObject): OrType {
        val a = parseAnnotations(obj)
        return OrType(
            orTypes = obj["or"]?.jsonArray?.map { parseNode(it) } ?: emptyList(),
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseAndType(obj: JsonObject): AndType {
        val a = parseAnnotations(obj)
        return AndType(
            andTypes = obj["and"]?.jsonArray?.map { parseNode(it) } ?: emptyList(),
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseTemplateLiteralType(obj: JsonObject): TemplateLiteralType {
        val a = parseAnnotations(obj)
        return TemplateLiteralType(
            format =
                obj["format"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("TemplateLiteralType missing 'format': $obj"),
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseConditionalType(obj: JsonObject): ConditionalType {
        val a = parseAnnotations(obj)
        val checkObj =
            obj["check"]?.jsonObject
                ?: throw IllegalArgumentException("Conditional missing 'check': $obj")
        val valueObj =
            obj["value"]?.jsonObject
                ?: throw IllegalArgumentException("Conditional missing 'value': $obj")
        return ConditionalType(
            check =
                ConditionalCheck(
                    left = parseNode(checkObj["left"] ?: throw IllegalArgumentException("check missing 'left'")),
                    right = parseNode(checkObj["right"] ?: throw IllegalArgumentException("check missing 'right'")),
                ),
            value =
                ConditionalValue(
                    trueValue = parseNode(valueObj["true"] ?: throw IllegalArgumentException("value missing 'true'")),
                    falseValue =
                        parseNode(
                            valueObj["false"]
                                ?: throw IllegalArgumentException("value missing 'false'"),
                        ),
                ),
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseFunctionType(obj: JsonObject): FunctionType {
        val a = parseAnnotations(obj)
        return FunctionType(
            parameters =
                obj["parameters"]?.jsonArray?.map { parseFunctionParameter(it.jsonObject) }
                    ?: emptyList(),
            returnType = obj["returnType"]?.let { parseNode(it) },
            name = a.name,
            title = a.title,
            description = a.description,
            examples = a.examples,
            default = a.default,
            see = a.see,
            comment = a.comment,
            meta = a.meta,
            source = a.source,
            genericTokens = a.genericTokens,
        )
    }

    private fun parseFunctionParameter(obj: JsonObject): FunctionParameter =
        FunctionParameter(
            name =
                obj["name"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("FunctionParameter missing 'name': $obj"),
            type = parseNode(obj["type"] ?: throw IllegalArgumentException("Parameter missing 'type'")),
            optional = obj["optional"]?.jsonPrimitive?.booleanOrNull,
            default = obj["default"]?.takeIf { it !is JsonNull }?.let { parseNode(it) },
        )

    private fun parseProperties(element: JsonElement?): Map<String, ObjectProperty> {
        if (element == null || element is JsonNull) return emptyMap()
        return element.jsonObject.mapValues { (_, propElement) ->
            val propObj = propElement.jsonObject
            ObjectProperty(
                required = propObj["required"]?.jsonPrimitive?.boolean ?: false,
                node = parseNode(propObj["node"] ?: throw IllegalArgumentException("Property missing 'node'")),
            )
        }
    }

    private fun parseParamTypeNode(obj: JsonObject): ParamTypeNode =
        ParamTypeNode(
            symbol =
                obj["symbol"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("ParamTypeNode missing 'symbol': $obj"),
            constraints = obj["constraints"]?.takeIf { it !is JsonNull }?.let { parseNode(it) },
            default = obj["default"]?.takeIf { it !is JsonNull }?.let { parseNode(it) },
        )
}
