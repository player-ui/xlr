package com.intuit.playerui.xlr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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
@Serializable
sealed interface AdditionalItemsType {
    @Serializable
    data object None : AdditionalItemsType

    @Serializable
    data class Typed(
        val node: NodeType,
    ) : AdditionalItemsType
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
sealed interface NodeType : Annotated {
    val type: String
    val source: String?
    val genericTokens: List<ParamTypeNode>?
}

@Serializable
@SerialName("string")
data class StringType(
    override val type: String = "string",
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
    override val type: String = "number",
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
    override val type: String = "boolean",
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
    override val type: String = "null",
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
    override val type: String = "any",
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
    override val type: String = "unknown",
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
    override val type: String = "undefined",
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
    override val type: String = "void",
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
    override val type: String = "never",
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
    override val type: String = "ref",
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
    override val type: String = "object",
    val properties: Map<String, ObjectProperty> = emptyMap(),
    val extends: RefType? = null,
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
    override val type: String = "array",
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
    override val type: String = "tuple",
    val elementTypes: List<TupleMember>,
    val minItems: Int,
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
    override val type: String = "record",
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

@Serializable
@SerialName("or")
data class OrType(
    override val type: String = "or",
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

@Serializable
@SerialName("and")
data class AndType(
    override val type: String = "and",
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
    override val type: String = "template",
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
    override val type: String = "conditional",
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
    override val type: String = "function",
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

/**
 * Polymorphic serializers module for NodeType hierarchy.
 */
val xlrSerializersModule: SerializersModule =
    SerializersModule {
        polymorphic(NodeType::class) {
            subclass(StringType::class)
            subclass(NumberType::class)
            subclass(BooleanType::class)
            subclass(NullType::class)
            subclass(AnyType::class)
            subclass(UnknownType::class)
            subclass(UndefinedType::class)
            subclass(VoidType::class)
            subclass(NeverType::class)
            subclass(RefType::class)
            subclass(ObjectType::class)
            subclass(ArrayType::class)
            subclass(TupleType::class)
            subclass(RecordType::class)
            subclass(OrType::class)
            subclass(AndType::class)
            subclass(TemplateLiteralType::class)
            subclass(ConditionalType::class)
            subclass(FunctionType::class)
        }
    }
