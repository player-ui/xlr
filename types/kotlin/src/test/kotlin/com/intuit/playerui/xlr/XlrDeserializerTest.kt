package com.intuit.playerui.xlr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XlrDeserializerTest {
    private val fixtureJson: String by lazy {
        this::class.java
            .getResourceAsStream("/test.json")!!
            .bufferedReader()
            .readText()
    }

    @Test
    fun `deserializes document name and source`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        assertEquals("ChoiceAsset", doc.name)
        assertTrue(doc.source.contains("choice/types.ts"))
    }

    @Test
    fun `deserializes document objectType as object`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        assertEquals("object", doc.objectType.type)
    }

    @Test
    fun `toObjectType returns equivalent ObjectType`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val objType = doc.toObjectType()
        assertEquals(doc.objectType, objType)
    }

    @Test
    fun `deserializes properties map with correct keys`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val props = doc.objectType.properties
        assertTrue(props.containsKey("title"))
        assertTrue(props.containsKey("note"))
        assertTrue(props.containsKey("binding"))
        assertTrue(props.containsKey("items"))
        assertTrue(props.containsKey("metaData"))
        assertEquals(5, props.size)
    }

    @Test
    fun `deserializes title property as RefType`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val titleProp = doc.objectType.properties["title"]!!
        assertEquals(false, titleProp.required)
        val node = assertIs<RefType>(titleProp.node)
        assertEquals("AssetWrapper<AnyTextAsset>", node.ref)
        assertNotNull(node.genericArguments)
        assertEquals(1, node.genericArguments!!.size)
        val ga = assertIs<RefType>(node.genericArguments!!.first())
        assertEquals("AnyTextAsset", ga.ref)
    }

    @Test
    fun `deserializes binding property as RefType`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val bindingNode = assertIs<RefType>(doc.objectType.properties["binding"]!!.node)
        assertEquals("Binding", bindingNode.ref)
        assertEquals("ChoiceAsset.binding", bindingNode.title)
        assertEquals("The location in the data-model to store the data", bindingNode.description)
    }

    @Test
    fun `deserializes items property as ArrayType with nested ObjectType`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val itemsNode = assertIs<ArrayType>(doc.objectType.properties["items"]!!.node)
        val elementType = assertIs<ObjectType>(itemsNode.elementType)
        assertTrue(elementType.properties.containsKey("id"))
        assertTrue(elementType.properties.containsKey("label"))
        assertTrue(elementType.properties.containsKey("value"))
    }

    @Test
    fun `deserializes nested ChoiceItem id as required StringType`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val itemsNode = assertIs<ArrayType>(doc.objectType.properties["items"]!!.node)
        val choiceItem = assertIs<ObjectType>(itemsNode.elementType)
        val idProp = choiceItem.properties["id"]!!
        assertEquals(true, idProp.required)
        assertIs<StringType>(idProp.node)
    }

    @Test
    fun `deserializes nested ValueType as OrType with 4 members`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val itemsNode = assertIs<ArrayType>(doc.objectType.properties["items"]!!.node)
        val choiceItem = assertIs<ObjectType>(itemsNode.elementType)
        val valueNode = assertIs<OrType>(choiceItem.properties["value"]!!.node)
        assertEquals(4, valueNode.orTypes.size)
        assertIs<StringType>(valueNode.orTypes[0])
        assertIs<NumberType>(valueNode.orTypes[1])
        assertIs<BooleanType>(valueNode.orTypes[2])
        assertIs<NullType>(valueNode.orTypes[3])
    }

    @Test
    fun `deserializes BeaconDataType as OrType with RecordType`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val metaDataNode = assertIs<ObjectType>(doc.objectType.properties["metaData"]!!.node)
        val beaconNode = assertIs<OrType>(metaDataNode.properties["beacon"]!!.node)
        assertEquals(2, beaconNode.orTypes.size)
        assertIs<StringType>(beaconNode.orTypes[0])
        val record = assertIs<RecordType>(beaconNode.orTypes[1])
        assertIs<StringType>(record.keyType)
        assertIs<AnyType>(record.valueType)
    }

    @Test
    fun `deserializes generic tokens on document`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        assertNotNull(doc.genericTokens)
        assertEquals(1, doc.genericTokens!!.size)
        val token = doc.genericTokens!!.first()
        assertEquals("AnyTextAsset", token.symbol)
        val constraint = assertIs<RefType>(token.constraints)
        assertEquals("Asset", constraint.ref)
        val default = assertIs<RefType>(token.default)
        assertEquals("Asset", default.ref)
    }

    @Test
    fun `deserializes generic tokens on nested ChoiceItem`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val itemsNode = assertIs<ArrayType>(doc.objectType.properties["items"]!!.node)
        val choiceItem = assertIs<ObjectType>(itemsNode.elementType)
        assertNotNull(choiceItem.genericTokens)
        assertEquals(1, choiceItem.genericTokens!!.size)
        val token = choiceItem.genericTokens!!.first()
        assertEquals("AnyTextAsset", token.symbol)
        val constraint = assertIs<RefType>(token.constraints)
        assertEquals("Asset", constraint.ref)
        val default = assertIs<RefType>(token.default)
        assertEquals("Asset", default.ref)
    }

    @Test
    fun `deserializes source on nested ChoiceItem`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val itemsNode = assertIs<ArrayType>(doc.objectType.properties["items"]!!.node)
        val choiceItem = assertIs<ObjectType>(itemsNode.elementType)
        assertNotNull(choiceItem.source)
        assertTrue(choiceItem.source!!.contains("choice/types.ts"))
    }

    @Test
    fun `deserializes source on nested ValueType`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val itemsNode = assertIs<ArrayType>(doc.objectType.properties["items"]!!.node)
        val choiceItem = assertIs<ObjectType>(itemsNode.elementType)
        val valueNode = assertIs<OrType>(choiceItem.properties["value"]!!.node)
        assertNotNull(valueNode.source)
        assertTrue(valueNode.source!!.contains("choice/types.ts"))
    }

    @Test
    fun `deserializes source on nested BeaconMetaData`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val metaDataNode = assertIs<ObjectType>(doc.objectType.properties["metaData"]!!.node)
        assertNotNull(metaDataNode.source)
        assertTrue(metaDataNode.source!!.contains("beacon"))
    }

    @Test
    fun `deserializes source on nested BeaconDataType`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val metaDataNode = assertIs<ObjectType>(doc.objectType.properties["metaData"]!!.node)
        val beaconNode = assertIs<OrType>(metaDataNode.properties["beacon"]!!.node)
        assertNotNull(beaconNode.source)
        assertTrue(beaconNode.source!!.contains("beacon"))
    }

    @Test
    fun `root objectType carries source and genericTokens`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        assertNotNull(doc.objectType.source)
        assertTrue(doc.objectType.source!!.contains("choice/types.ts"))
        assertNotNull(doc.objectType.genericTokens)
        assertEquals(1, doc.objectType.genericTokens!!.size)
        assertEquals(
            "AnyTextAsset",
            doc.objectType.genericTokens!!
                .first()
                .symbol,
        )
    }

    @Test
    fun `nodes without source or genericTokens default to null`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val bindingNode = assertIs<RefType>(doc.objectType.properties["binding"]!!.node)
        assertNull(bindingNode.source)
        assertNull(bindingNode.genericTokens)
    }

    @Test
    fun `deserializes extends clause with Asset choice`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val ext = doc.objectType.extends
        assertNotNull(ext)
        assertEquals("Asset<\"choice\">", ext.ref)
        assertNotNull(ext.genericArguments)
        assertEquals(1, ext.genericArguments!!.size)
        val arg = assertIs<StringType>(ext.genericArguments!!.first())
        assertEquals("choice", arg.const)
    }

    @Test
    fun `deserializes additionalProperties as None for false`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        assertEquals(AdditionalItemsType.None, doc.objectType.additionalProperties)
    }

    @Test
    fun `deserializes annotation title on document`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        assertEquals("ChoiceAsset", doc.objectType.title)
    }

    @Test
    fun `deserializes annotation description on document`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        assertNotNull(doc.objectType.description)
        assertTrue(doc.objectType.description!!.startsWith("A choice asset"))
    }

    @Test
    fun `deserializes annotation title on nested nodes`() {
        val doc = XlrDeserializer.deserialize(fixtureJson)
        val titleNode = assertIs<RefType>(doc.objectType.properties["title"]!!.node)
        assertEquals("ChoiceAsset.title", titleNode.title)
    }

    @Test
    fun `parseNode throws for missing type field`() {
        val json =
            kotlinx.serialization.json.Json
                .parseToJsonElement("""{"ref": "foo"}""")
        assertFailsWith<IllegalArgumentException> {
            XlrDeserializer.parseNode(json)
        }
    }

    @Test
    fun `parseNode throws for unknown type`() {
        val json =
            kotlinx.serialization.json.Json
                .parseToJsonElement("""{"type": "foobar"}""")
        assertFailsWith<IllegalArgumentException> {
            XlrDeserializer.parseNode(json)
        }
    }

    @Test
    fun `parseNode handles JsonNull as NullType`() {
        val node = XlrDeserializer.parseNode(kotlinx.serialization.json.JsonNull)
        assertIs<NullType>(node)
    }

    @Test
    fun `parseNode deserializes simple string type`() {
        val json =
            kotlinx.serialization.json.Json
                .parseToJsonElement("""{"type": "string", "const": "hello"}""")
        val node = assertIs<StringType>(XlrDeserializer.parseNode(json))
        assertEquals("hello", node.const)
    }

    @Test
    fun `parseNode deserializes string type with enum`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """{"type": "string", "enum": ["a", "b", "c"]}""",
            )
        val node = assertIs<StringType>(XlrDeserializer.parseNode(json))
        assertEquals(listOf("a", "b", "c"), node.enum)
    }

    @Test
    fun `parseNode deserializes number type with enum`() {
        val json =
            kotlinx.serialization.json.Json
                .parseToJsonElement("""{"type": "number", "enum": [1.0, 2.0, 3.0]}""")
        val node = assertIs<NumberType>(XlrDeserializer.parseNode(json))
        assertEquals(listOf(1.0, 2.0, 3.0), node.enum)
    }

    @Test
    fun `parseNode deserializes boolean type with const`() {
        val json =
            kotlinx.serialization.json.Json
                .parseToJsonElement("""{"type": "boolean", "const": true}""")
        val node = assertIs<BooleanType>(XlrDeserializer.parseNode(json))
        assertEquals(true, node.const)
    }

    @Test
    fun `parseNode deserializes template literal type`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """{"type": "template", "format": "hello_\\d+"}""",
            )
        val node = assertIs<TemplateLiteralType>(XlrDeserializer.parseNode(json))
        assertEquals("hello_\\d+", node.format)
    }

    @Test
    fun `parseNode deserializes conditional type with structured check and value`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """
                {
                    "type": "conditional",
                    "check": {"left": {"type": "string"}, "right": {"type": "number"}},
                    "value": {"true": {"type": "boolean"}, "false": {"type": "null"}}
                }
                """.trimIndent(),
            )
        val node = assertIs<ConditionalType>(XlrDeserializer.parseNode(json))
        assertIs<StringType>(node.check.left)
        assertIs<NumberType>(node.check.right)
        assertIs<BooleanType>(node.value.trueValue)
        assertIs<NullType>(node.value.falseValue)
    }

    @Test
    fun `parseNode deserializes function type`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """
                {
                    "type": "function",
                    "parameters": [
                        {"name": "x", "type": {"type": "string"}},
                        {"name": "y", "type": {"type": "number"}, "optional": true}
                    ],
                    "returnType": {"type": "boolean"}
                }
                """.trimIndent(),
            )
        val node = assertIs<FunctionType>(XlrDeserializer.parseNode(json))
        assertEquals(2, node.parameters.size)
        assertEquals("x", node.parameters[0].name)
        assertIs<StringType>(node.parameters[0].type)
        assertNull(node.parameters[0].optional)
        assertEquals("y", node.parameters[1].name)
        assertEquals(true, node.parameters[1].optional)
        assertIs<BooleanType>(node.returnType)
    }

    @Test
    fun `parseNode deserializes function type with parameter default`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """
                {
                    "type": "function",
                    "parameters": [
                        {"name": "x", "type": {"type": "string"}, "default": {"type": "string", "const": "hi"}}
                    ]
                }
                """.trimIndent(),
            )
        val node = assertIs<FunctionType>(XlrDeserializer.parseNode(json))
        assertEquals(1, node.parameters.size)
        val defaultNode = assertIs<StringType>(node.parameters[0].default)
        assertEquals("hi", defaultNode.const)
    }

    @Test
    fun `parseNode deserializes ref type with property`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """{"type": "ref", "ref": "Foo", "property": "bar"}""",
            )
        val node = assertIs<RefType>(XlrDeserializer.parseNode(json))
        assertEquals("Foo", node.ref)
        assertEquals("bar", node.property)
    }

    @Test
    fun `parseNode deserializes tuple type`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """
                {
                    "type": "tuple",
                    "elementTypes": [
                        {"name": "first", "type": {"type": "string"}},
                        {"name": "second", "type": {"type": "number"}, "optional": true}
                    ],
                    "minItems": 1,
                    "additionalItems": false
                }
                """.trimIndent(),
            )
        val node = assertIs<TupleType>(XlrDeserializer.parseNode(json))
        assertEquals(2, node.elementTypes.size)
        assertEquals("first", node.elementTypes[0].name)
        assertIs<StringType>(node.elementTypes[0].type)
        assertEquals(true, node.elementTypes[1].optional)
        assertEquals(1, node.minItems)
        assertEquals(AdditionalItemsType.None, node.additionalItems)
    }

    @Test
    fun `parseNode deserializes tuple member without name`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """
                {
                    "type": "tuple",
                    "elementTypes": [{"type": {"type": "string"}}],
                    "minItems": 1
                }
                """.trimIndent(),
            )
        val node = assertIs<TupleType>(XlrDeserializer.parseNode(json))
        assertEquals(1, node.elementTypes.size)
        assertNull(node.elementTypes[0].name)
        assertIs<StringType>(node.elementTypes[0].type)
    }

    @Test
    fun `parseNode deserializes and type`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """{"type": "and", "and": [{"type": "string"}, {"type": "number"}]}""",
            )
        val node = assertIs<AndType>(XlrDeserializer.parseNode(json))
        assertEquals(2, node.andTypes.size)
        assertIs<StringType>(node.andTypes[0])
        assertIs<NumberType>(node.andTypes[1])
    }

    @Test
    fun `parseNode deserializes all simple types`() {
        val simpleTypes = listOf("any", "unknown", "undefined", "void", "never", "null")
        for (typeName in simpleTypes) {
            val json =
                kotlinx.serialization.json.Json
                    .parseToJsonElement("""{"type": "$typeName"}""")
            val node = XlrDeserializer.parseNode(json)
            assertEquals(typeName, node.type, "Failed for type: $typeName")
        }
    }

    @Test
    fun `additionalProperties Typed parses NodeType`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """
                {
                    "type": "object",
                    "properties": {},
                    "additionalProperties": {"type": "string"}
                }
                """.trimIndent(),
            )
        val node = assertIs<ObjectType>(XlrDeserializer.parseNode(json))
        val ap = assertIs<AdditionalItemsType.Typed>(node.additionalProperties)
        assertIs<StringType>(ap.node)
    }

    @Test
    fun `additionalProperties null becomes None`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """{"type": "object", "properties": {}}""",
            )
        val node = assertIs<ObjectType>(XlrDeserializer.parseNode(json))
        assertEquals(AdditionalItemsType.None, node.additionalProperties)
    }

    @Test
    fun `parseNode preserves source and genericTokens on node`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """
                {
                    "type": "object",
                    "properties": {},
                    "source": "foo.ts",
                    "name": "Foo",
                    "genericTokens": [
                        {"symbol": "T", "constraints": {"type": "ref", "ref": "Bar"}}
                    ]
                }
                """.trimIndent(),
            )
        val node = assertIs<ObjectType>(XlrDeserializer.parseNode(json))
        assertEquals("foo.ts", node.source)
        assertEquals("Foo", node.name)
        assertNotNull(node.genericTokens)
        assertEquals(1, node.genericTokens!!.size)
        assertEquals("T", node.genericTokens!!.first().symbol)
    }

    @Test
    fun `throws on RefType missing ref`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """{"type": "ref"}""",
            )
        assertFailsWith<IllegalArgumentException> {
            XlrDeserializer.parseNode(json)
        }
    }

    @Test
    fun `throws on TemplateLiteralType missing format`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """{"type": "template"}""",
            )
        assertFailsWith<IllegalArgumentException> {
            XlrDeserializer.parseNode(json)
        }
    }

    @Test
    fun `throws on ArrayType missing elementType`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """{"type": "array"}""",
            )
        assertFailsWith<IllegalArgumentException> {
            XlrDeserializer.parseNode(json)
        }
    }

    @Test
    fun `throws on RecordType missing keyType`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """{"type": "record", "valueType": {"type": "any"}}""",
            )
        assertFailsWith<IllegalArgumentException> {
            XlrDeserializer.parseNode(json)
        }
    }

    @Test
    fun `throws on ConditionalType missing check`() {
        val json =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """
                {
                    "type": "conditional",
                    "value": {"true": {"type": "string"}, "false": {"type": "null"}}
                }
                """.trimIndent(),
            )
        assertFailsWith<IllegalArgumentException> {
            XlrDeserializer.parseNode(json)
        }
    }
}
