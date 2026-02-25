@file:OptIn(ExperimentalSerializationApi::class)
@file:UseSerializers(WholeNumberDoubleSerializer::class)

package com.intuit.playerui.xlr

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Interface for annotation fields on XLR types.
 * Mirrors the JS Annotations interface with all 8 annotation properties.
 */
interface Annotated {
    val name: String?
    val title: String?
    val description: String?
    val examples: JsonElement?
    val default: JsonElement?
    val see: JsonElement?
    val comment: String?
    val meta: Map<String, String>?
}

/**
 * Sealed type representing `false | NodeType` for additional properties/items.
 */
@Serializable(with = AdditionalItemsTypeSerializer::class)
sealed interface AdditionalItemsType {
    data object None : AdditionalItemsType

    data class Typed(
        val node: NodeType,
    ) : AdditionalItemsType
}

object AdditionalItemsTypeSerializer : KSerializer<AdditionalItemsType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AdditionalItemsType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AdditionalItemsType) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("AdditionalItemsTypeSerializer only supports JSON format")
        when (value) {
            is AdditionalItemsType.None -> jsonEncoder.encodeJsonElement(JsonPrimitive(false))
            is AdditionalItemsType.Typed ->
                jsonEncoder.encodeJsonElement(
                    jsonEncoder.json.encodeToJsonElement(NodeType.serializer(), value.node),
                )
        }
    }

    override fun deserialize(decoder: Decoder): AdditionalItemsType {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("AdditionalItemsTypeSerializer only supports JSON format")
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return AdditionalItemsType.None
        if (element is JsonPrimitive && element.booleanOrNull == false) return AdditionalItemsType.None
        return AdditionalItemsType.Typed(
            jsonDecoder.json.decodeFromJsonElement(NodeType.serializer(), element),
        )
    }
}

/**
 * Structured check for conditional types: `{ left: NodeType, right: NodeType }`.
 */
@Serializable
data class ConditionalCheck(
    val left: NodeType,
    val right: NodeType,
)

/**
 * Structured value for conditional types: `{ true: NodeType, false: NodeType }`.
 */
@Serializable
data class ConditionalValue(
    @SerialName("true") val trueValue: NodeType,
    @SerialName("false") val falseValue: NodeType,
)

/**
 * Base sealed interface for all XLR node types.
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface NodeType : Annotated {
    val type: String
    val source: String?
    val genericTokens: List<ParamTypeNode>?
}

@Serializable
@SerialName("string")
data class StringType(
    @Transient override val type: String = "string",
    val const: String? = null,
    val enum: List<String>? = null,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("number")
data class NumberType(
    @Transient override val type: String = "number",
    val const: Double? = null,
    val enum: List<Double>? = null,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("boolean")
data class BooleanType(
    @Transient override val type: String = "boolean",
    val const: Boolean? = null,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("null")
data class NullType(
    @Transient override val type: String = "null",
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("any")
data class AnyType(
    @Transient override val type: String = "any",
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("unknown")
data class UnknownType(
    @Transient override val type: String = "unknown",
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("undefined")
data class UndefinedType(
    @Transient override val type: String = "undefined",
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("void")
data class VoidType(
    @Transient override val type: String = "void",
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("never")
data class NeverType(
    @Transient override val type: String = "never",
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("ref")
data class RefType(
    @Transient override val type: String = "ref",
    val ref: String,
    val genericArguments: List<NodeType>? = null,
    val property: String? = null,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
data class ObjectProperty(
    val required: Boolean,
    val node: NodeType,
)

@Serializable
@SerialName("object")
data class ObjectType(
    @Transient override val type: String = "object",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val properties: Map<String, ObjectProperty> = emptyMap(),
    val extends: RefType? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val additionalProperties: AdditionalItemsType = AdditionalItemsType.None,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("array")
data class ArrayType(
    @Transient override val type: String = "array",
    val elementType: NodeType,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
data class TupleMember(
    val name: String? = null,
    val type: NodeType,
    val optional: Boolean? = null,
)

@Serializable
@SerialName("tuple")
data class TupleType(
    @Transient override val type: String = "tuple",
    val elementTypes: List<TupleMember>,
    val minItems: Int,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val additionalItems: AdditionalItemsType = AdditionalItemsType.None,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("record")
data class RecordType(
    @Transient override val type: String = "record",
    val keyType: NodeType,
    val valueType: NodeType,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

// Class-level @SerialName("or") sets the polymorphic discriminator value ("type": "or"),
// while property-level @SerialName("or") aliases the Kotlin field `orTypes` to JSON key "or".
@Serializable
@SerialName("or")
data class OrType(
    @Transient override val type: String = "or",
    @SerialName("or")
    val orTypes: List<NodeType>,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

// Class-level @SerialName("and") sets the polymorphic discriminator value ("type": "and"),
// while property-level @SerialName("and") aliases the Kotlin field `andTypes` to JSON key "and".
@Serializable
@SerialName("and")
data class AndType(
    @Transient override val type: String = "and",
    @SerialName("and")
    val andTypes: List<NodeType>,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("template")
data class TemplateLiteralType(
    @Transient override val type: String = "template",
    val format: String,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
@SerialName("conditional")
data class ConditionalType(
    @Transient override val type: String = "conditional",
    val check: ConditionalCheck,
    val value: ConditionalValue,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
data class FunctionParameter(
    val name: String,
    val type: NodeType,
    val optional: Boolean? = null,
    val default: NodeType? = null,
)

@Serializable
@SerialName("function")
data class FunctionType(
    @Transient override val type: String = "function",
    val parameters: List<FunctionParameter>,
    val returnType: NodeType? = null,
    override val name: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val examples: JsonElement? = null,
    override val default: JsonElement? = null,
    override val see: JsonElement? = null,
    override val comment: String? = null,
    override val meta: Map<String, String>? = null,
    override val source: String? = null,
    override val genericTokens: List<ParamTypeNode>? = null,
) : NodeType

@Serializable
data class ParamTypeNode(
    val symbol: String,
    val constraints: NodeType? = null,
    val default: NodeType? = null,
)

/**
 * Named type wrapper that adds name and source information to any node type.
 */
@Serializable
data class NamedType<T : NodeType>(
    @SerialName("name") val typeName: String,
    val source: String,
    val genericTokens: List<ParamTypeNode>? = null,
    val node: T,
)

/**
 * Named type wrapper with required generic tokens.
 */
@Serializable
data class NamedTypeWithGenerics<T : NodeType>(
    @SerialName("name") val typeName: String,
    val source: String,
    val genericTokens: List<ParamTypeNode>,
    val node: T,
)

/**
 * Node type with generic tokens attached.
 */
@Serializable
data class NodeTypeWithGenerics<T : NodeType>(
    val node: T,
    val genericTokens: List<ParamTypeNode>,
)

/**
 * Complete XLR document representing a named object type (asset definition).
 */
@Serializable
data class XlrDocument(
    val name: String,
    val source: String,
    val objectType: ObjectType,
    val genericTokens: List<ParamTypeNode>? = null,
) {
    fun toObjectType(): ObjectType = objectType
}

object WholeNumberDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("WholeNumberDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("WholeNumberDoubleSerializer only supports JSON format")
        if (value.isFinite() && value % 1.0 == 0.0) {
            jsonEncoder.encodeJsonElement(JsonPrimitive(value.toLong()))
        } else {
            jsonEncoder.encodeJsonElement(JsonPrimitive(value))
        }
    }

    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()
}
