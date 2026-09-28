package com.intuit.playerui.xlr

import kotlinx.serialization.Serializable

@Serializable
data class Capability(
    val name: String,
    val provides: List<String>,
)

@Serializable
data class Manifest(
    val pluginName: String,
    val capabilities: Map<String, List<String>>? = null,
    val customPrimitives: List<String>? = null,
)

@Serializable
data class TSManifest(
    val pluginName: String,
    val capabilities: Map<String, List<NamedType<NodeType>>>,
)

sealed interface TransformInput {
    data class Named(
        val namedType: NamedType<NodeType>,
    ) : TransformInput

    data class Anonymous(
        val nodeType: NodeType,
    ) : TransformInput
}

sealed interface TransformOutput {
    data class Named(
        val namedType: NamedType<NodeType>,
    ) : TransformOutput

    data class Anonymous(
        val nodeType: NodeType,
    ) : TransformOutput
}

fun interface TransformFunction {
    fun transform(
        input: TransformInput,
        capabilityType: String,
    ): TransformOutput
}
