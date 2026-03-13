package com.intuit.playerui.xlr

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class XlrUtilityTest {
    // Intentionally omits isLenient=true (present in production xlrJson) to test with stricter parsing.
    private val json =
        Json {
            encodeDefaults = false
            ignoreUnknownKeys = true
        }

    @Test
    fun `Capability construction`() {
        val cap = Capability(name = "MyCapability", provides = listOf("TypeA", "TypeB"))
        assertEquals("MyCapability", cap.name)
        assertEquals(listOf("TypeA", "TypeB"), cap.provides)
    }

    @Test
    fun `Capability serialization round-trip`() {
        val original = Capability(name = "MyCap", provides = listOf("A", "B"))
        val encoded = json.encodeToString(Capability.serializer(), original)
        val decoded = json.decodeFromString(Capability.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `Capability with empty provides list`() {
        val cap = Capability(name = "EmptyCap", provides = emptyList())
        assertEquals(emptyList(), cap.provides)
    }

    @Test
    fun `Manifest construction with all fields`() {
        val manifest =
            Manifest(
                pluginName = "TestPlugin",
                capabilities = mapOf("cap1" to listOf("TypeA")),
                customPrimitives = listOf("CustomString"),
            )
        assertEquals("TestPlugin", manifest.pluginName)
        assertEquals(mapOf("cap1" to listOf("TypeA")), manifest.capabilities)
        assertEquals(listOf("CustomString"), manifest.customPrimitives)
    }

    @Test
    fun `Manifest with defaults`() {
        val manifest = Manifest(pluginName = "TestPlugin")
        assertEquals("TestPlugin", manifest.pluginName)
        assertNull(manifest.capabilities)
        assertNull(manifest.customPrimitives)
    }

    @Test
    fun `Manifest serialization round-trip with all fields`() {
        val original =
            Manifest(
                pluginName = "TestPlugin",
                capabilities = mapOf("cap1" to listOf("TypeA", "TypeB")),
                customPrimitives = listOf("CustomString"),
            )
        val encoded = json.encodeToString(Manifest.serializer(), original)
        val decoded = json.decodeFromString(Manifest.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `Manifest serialization omits null optionals`() {
        val manifest = Manifest(pluginName = "TestPlugin")
        val encoded = json.encodeToJsonElement(Manifest.serializer(), manifest)
        val obj = encoded.jsonObject
        assertEquals(setOf("pluginName"), obj.keys)
    }

    @Test
    fun `TSManifest construction`() {
        val namedType: NamedType<NodeType> =
            NamedType(
                typeName = "MyType",
                source = "test.ts",
                node = StringType(const = "value"),
            )
        val manifest =
            TSManifest(
                pluginName = "TestPlugin",
                capabilities = mapOf("cap1" to listOf(namedType)),
            )
        assertEquals("TestPlugin", manifest.pluginName)
        assertEquals(1, manifest.capabilities.size)
        assertEquals(1, manifest.capabilities["cap1"]?.size)
    }

    @Test
    fun `TSManifest serialization round-trip`() {
        val namedType: NamedType<NodeType> =
            NamedType(
                typeName = "MyType",
                source = "test.ts",
                node = ObjectType(),
            )
        val original =
            TSManifest(
                pluginName = "TestPlugin",
                capabilities = mapOf("cap1" to listOf(namedType)),
            )
        val encoded = json.encodeToString(TSManifest.serializer(), original)
        val decoded = json.decodeFromString(TSManifest.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `TransformInput Named construction`() {
        val namedType: NamedType<NodeType> = NamedType(typeName = "Foo", source = "foo.ts", node = StringType())
        val input = TransformInput.Named(namedType)
        assertEquals(namedType, input.namedType)
    }

    @Test
    fun `TransformInput Anonymous construction`() {
        val nodeType: NodeType = StringType(const = "hello")
        val input = TransformInput.Anonymous(nodeType)
        assertEquals(nodeType, input.nodeType)
    }

    @Test
    fun `TransformInput sealed variants distinguished via is checks`() {
        val named: TransformInput =
            TransformInput.Named(
                NamedType<NodeType>(typeName = "Foo", source = "foo.ts", node = StringType()),
            )
        val anonymous: TransformInput = TransformInput.Anonymous(StringType())
        assertIs<TransformInput.Named>(named)
        assertIs<TransformInput.Anonymous>(anonymous)
    }

    @Test
    fun `TransformInput Named data class equality`() {
        val nt: NamedType<NodeType> = NamedType(typeName = "Foo", source = "foo.ts", node = StringType())
        val a = TransformInput.Named(nt)
        val b = TransformInput.Named(nt)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `TransformInput Anonymous data class equality`() {
        val a = TransformInput.Anonymous(StringType(const = "x"))
        val b = TransformInput.Anonymous(StringType(const = "x"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `TransformOutput Named construction`() {
        val namedType: NamedType<NodeType> = NamedType(typeName = "Bar", source = "bar.ts", node = NumberType())
        val output = TransformOutput.Named(namedType)
        assertEquals(namedType, output.namedType)
    }

    @Test
    fun `TransformOutput Anonymous construction`() {
        val nodeType: NodeType = NumberType(const = 42.0)
        val output = TransformOutput.Anonymous(nodeType)
        assertEquals(nodeType, output.nodeType)
    }

    @Test
    fun `TransformOutput sealed variants distinguished via is checks`() {
        val named: TransformOutput =
            TransformOutput.Named(
                NamedType<NodeType>(typeName = "Bar", source = "bar.ts", node = NumberType()),
            )
        val anonymous: TransformOutput = TransformOutput.Anonymous(NumberType())
        assertIs<TransformOutput.Named>(named)
        assertIs<TransformOutput.Anonymous>(anonymous)
    }

    @Test
    fun `TransformOutput data class equality`() {
        val nt: NamedType<NodeType> = NamedType(typeName = "Bar", source = "bar.ts", node = NumberType())
        val a = TransformOutput.Named(nt)
        val b = TransformOutput.Named(nt)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `TransformFunction invocation with Named input`() {
        val transform =
            TransformFunction { input, capabilityType ->
                val named = input as TransformInput.Named
                TransformOutput.Anonymous(
                    StringType(const = "${named.namedType.typeName}_$capabilityType"),
                )
            }
        val input =
            TransformInput.Named(
                NamedType<NodeType>(typeName = "Foo", source = "foo.ts", node = ObjectType()),
            )
        val output = transform.transform(input, "asset")
        val anonymous = assertIs<TransformOutput.Anonymous>(output)
        val strType = assertIs<StringType>(anonymous.nodeType)
        assertEquals("Foo_asset", strType.const)
    }

    @Test
    fun `TransformFunction invocation with Anonymous input`() {
        val transform =
            TransformFunction { input, _ ->
                val anon = input as TransformInput.Anonymous
                TransformOutput.Anonymous(anon.nodeType)
            }
        val input = TransformInput.Anonymous(NumberType(const = 42.0))
        val output = transform.transform(input, "binding")
        val anonymous = assertIs<TransformOutput.Anonymous>(output)
        assertIs<NumberType>(anonymous.nodeType)
    }
}
