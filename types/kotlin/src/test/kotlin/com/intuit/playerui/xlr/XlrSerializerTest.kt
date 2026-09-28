package com.intuit.playerui.xlr

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class XlrSerializerTest {
    private val fixtureJson: String get() = TestFixtures.choiceAssetJson
    private val doc by lazy { XlrDeserializer.deserialize(fixtureJson) }

    @Test
    fun `round-trip deserialize-serialize-deserialize produces equal documents`() {
        val serialized = XlrSerializer.serialize(doc)
        val doc2 = XlrDeserializer.deserialize(serialized)
        assertEquals(doc, doc2)
    }

    @Test
    fun `serializes document name and source`() {
        val jsonObj = XlrSerializer.serializeDocument(doc)
        assertEquals("ChoiceAsset", jsonObj["name"]?.jsonPrimitive?.content)
        assertTrue(
            jsonObj["source"]
                ?.jsonPrimitive
                ?.content
                ?.contains("choice/types.ts") == true,
        )
    }

    @Test
    fun `serializes document genericTokens`() {
        val jsonObj = XlrSerializer.serializeDocument(doc)
        val tokens = jsonObj["genericTokens"]
        assertNotNull(tokens)
    }

    @Test
    fun `serializes StringType`() {
        val node = StringType(const = "hello", name = "MyStr")
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("string", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("hello", jsonObj["const"]?.jsonPrimitive?.content)
        assertEquals("MyStr", jsonObj["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes StringType with enum`() {
        val node = StringType(enum = listOf("a", "b"))
        val jsonObj = XlrSerializer.serializeNode(node)
        val arr = jsonObj["enum"]?.jsonArray
        assertNotNull(arr)
        assertEquals(2, arr.size)
        assertEquals("a", arr[0].jsonPrimitive.content)
        assertEquals("b", arr[1].jsonPrimitive.content)
    }

    @Test
    fun `serializes NumberType with enum`() {
        val node = NumberType(enum = listOf(1.0, 2.0))
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("number", jsonObj["type"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["enum"])
    }

    @Test
    fun `serializes NumberType integer const without decimal`() {
        val node = NumberType(const = 42.0)
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("42", jsonObj["const"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes NumberType fractional const with decimal`() {
        val node = NumberType(const = 3.14)
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("3.14", jsonObj["const"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes BooleanType with const`() {
        val node = BooleanType(const = true)
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("boolean", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("true", jsonObj["const"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes simple types`() {
        assertEquals(
            "null",
            XlrSerializer.serializeNode(NullType())["type"]?.jsonPrimitive?.content,
        )
        assertEquals(
            "any",
            XlrSerializer.serializeNode(AnyType())["type"]?.jsonPrimitive?.content,
        )
        assertEquals(
            "unknown",
            XlrSerializer.serializeNode(UnknownType())["type"]?.jsonPrimitive?.content,
        )
        assertEquals(
            "undefined",
            XlrSerializer.serializeNode(UndefinedType())["type"]?.jsonPrimitive?.content,
        )
        assertEquals(
            "void",
            XlrSerializer.serializeNode(VoidType())["type"]?.jsonPrimitive?.content,
        )
        assertEquals(
            "never",
            XlrSerializer.serializeNode(NeverType())["type"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun `serializes RefType with genericArguments`() {
        val node = RefType(ref = "Foo", genericArguments = listOf(StringType()))
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("ref", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("Foo", jsonObj["ref"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["genericArguments"])
    }

    @Test
    fun `serializes RefType with property`() {
        val node = RefType(ref = "Foo", property = "bar")
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("bar", jsonObj["property"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes ObjectType with properties and extends`() {
        val node =
            ObjectType(
                properties = mapOf("x" to ObjectProperty(required = true, node = StringType())),
                extends = RefType(ref = "Base"),
            )
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("object", jsonObj["type"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["properties"])
        assertNotNull(jsonObj["extends"])
    }

    @Test
    fun `serializes ObjectType with empty properties`() {
        val node = ObjectType()
        val jsonObj = XlrSerializer.serializeNode(node)
        assertTrue(jsonObj.containsKey("properties"))
        assertEquals(0, jsonObj["properties"]?.jsonObject?.size)
    }

    @Test
    fun `serializes ObjectType with typed additionalProperties`() {
        val node =
            ObjectType(
                additionalProperties = AdditionalItemsType.Typed(StringType()),
            )
        val jsonObj = XlrSerializer.serializeNode(node)
        val ap = jsonObj["additionalProperties"]?.jsonObject
        assertNotNull(ap)
        assertEquals("string", ap["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes ArrayType`() {
        val node = ArrayType(elementType = NumberType())
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("array", jsonObj["type"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["elementType"])
    }

    @Test
    fun `serializes TupleType`() {
        val node =
            TupleType(
                elementTypes = listOf(TupleMember(name = "a", type = StringType())),
                minItems = 1,
            )
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("tuple", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("1", jsonObj["minItems"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes TupleType with typed additionalItems`() {
        val node =
            TupleType(
                elementTypes = listOf(TupleMember(type = StringType())),
                minItems = 1,
                additionalItems = AdditionalItemsType.Typed(AnyType()),
            )
        val jsonObj = XlrSerializer.serializeNode(node)
        val ai = jsonObj["additionalItems"]?.jsonObject
        assertNotNull(ai)
        assertEquals("any", ai["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes TupleMember without name`() {
        val node =
            TupleType(
                elementTypes = listOf(TupleMember(type = StringType())),
                minItems = 1,
            )
        val jsonObj = XlrSerializer.serializeNode(node)
        val member = jsonObj["elementTypes"]?.jsonArray?.first()?.jsonObject
        assertNotNull(member)
        assertFalse(member.containsKey("name"))
    }

    @Test
    fun `serializes RecordType`() {
        val node = RecordType(keyType = StringType(), valueType = AnyType())
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("record", jsonObj["type"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["keyType"])
        assertNotNull(jsonObj["valueType"])
    }

    @Test
    fun `serializes OrType`() {
        val node = OrType(orTypes = listOf(StringType(), NumberType()))
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("or", jsonObj["type"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["or"])
    }

    @Test
    fun `serializes AndType`() {
        val node = AndType(andTypes = listOf(StringType(), NumberType()))
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("and", jsonObj["type"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["and"])
    }

    @Test
    fun `serializes TemplateLiteralType`() {
        val node = TemplateLiteralType(format = "hello_\\d+")
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("template", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("hello_\\d+", jsonObj["format"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes ConditionalType`() {
        val node =
            ConditionalType(
                check = ConditionalCheck(StringType(), NumberType()),
                value = ConditionalValue(BooleanType(), NullType()),
            )
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("conditional", jsonObj["type"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["check"])
        assertNotNull(jsonObj["value"])
    }

    @Test
    fun `serializes FunctionType`() {
        val node =
            FunctionType(
                parameters = listOf(FunctionParameter(name = "x", type = StringType(), optional = true)),
                returnType = BooleanType(),
            )
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("function", jsonObj["type"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["parameters"])
        assertNotNull(jsonObj["returnType"])
    }

    @Test
    fun `serializes FunctionType with parameter default`() {
        val node =
            FunctionType(
                parameters =
                    listOf(
                        FunctionParameter(
                            name = "x",
                            type = StringType(),
                            default = StringType(const = "hi"),
                        ),
                    ),
            )
        val jsonObj = XlrSerializer.serializeNode(node)
        val param = jsonObj["parameters"]?.jsonArray?.first()?.jsonObject
        assertNotNull(param)
        val defaultObj = param["default"]?.jsonObject
        assertNotNull(defaultObj)
        assertEquals("string", defaultObj["type"]?.jsonPrimitive?.content)
        assertEquals("hi", defaultObj["const"]?.jsonPrimitive?.content)
    }

    @Test
    fun `null optional fields are not emitted`() {
        val node = StringType()
        val jsonObj = XlrSerializer.serializeNode(node)
        assertFalse(jsonObj.containsKey("const"))
        assertFalse(jsonObj.containsKey("enum"))
        assertFalse(jsonObj.containsKey("name"))
        assertFalse(jsonObj.containsKey("title"))
        assertFalse(jsonObj.containsKey("description"))
        assertFalse(jsonObj.containsKey("source"))
        assertFalse(jsonObj.containsKey("genericTokens"))
        assertFalse(jsonObj.containsKey("examples"))
        assertFalse(jsonObj.containsKey("comment"))
        assertFalse(jsonObj.containsKey("meta"))
    }

    @Test
    fun `serializes source and genericTokens on node`() {
        val tokens = listOf(ParamTypeNode(symbol = "T", constraints = RefType(ref = "Base")))
        val node = ObjectType(source = "foo.ts", genericTokens = tokens, name = "Foo")
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("foo.ts", jsonObj["source"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["genericTokens"])
        assertEquals("Foo", jsonObj["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes annotations on nodes`() {
        val node =
            StringType(
                title = "MyTitle",
                description = "A string",
                comment = "Some comment",
                meta = mapOf("key" to "value"),
            )
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals("MyTitle", jsonObj["title"]?.jsonPrimitive?.content)
        assertEquals("A string", jsonObj["description"]?.jsonPrimitive?.content)
        assertEquals("Some comment", jsonObj["comment"]?.jsonPrimitive?.content)
        assertNotNull(jsonObj["meta"])
        assertEquals(
            "value",
            jsonObj["meta"]
                ?.jsonObject
                ?.get("key")
                ?.jsonPrimitive
                ?.content,
        )
    }

    @Test
    fun `round-trip ObjectType with empty properties`() {
        val original = ObjectType()
        val json = XlrSerializer.serializeNode(original)
        val deserialized = XlrDeserializer.parseNode(json)
        assertEquals(original, deserialized)
    }

    @Test
    fun `round-trip ObjectType with typed additionalProperties`() {
        val original =
            ObjectType(
                additionalProperties = AdditionalItemsType.Typed(StringType()),
            )
        val json = XlrSerializer.serializeNode(original)
        val deserialized = XlrDeserializer.parseNode(json)
        assertEquals(original, deserialized)
    }

    @Test
    fun `round-trip TupleType with typed additionalItems`() {
        val original =
            TupleType(
                elementTypes =
                    listOf(
                        TupleMember(name = "a", type = StringType()),
                        TupleMember(type = NumberType(), optional = true),
                    ),
                minItems = 1,
                additionalItems = AdditionalItemsType.Typed(AnyType()),
            )
        val json = XlrSerializer.serializeNode(original)
        val deserialized = XlrDeserializer.parseNode(json)
        assertEquals(original, deserialized)
    }

    @Test
    fun `round-trip nested ConditionalType`() {
        val original =
            ConditionalType(
                check =
                    ConditionalCheck(
                        left = OrType(orTypes = listOf(StringType(), NumberType())),
                        right = RefType(ref = "Foo"),
                    ),
                value =
                    ConditionalValue(
                        trueValue = ArrayType(elementType = BooleanType()),
                        falseValue = NullType(),
                    ),
            )
        val json = XlrSerializer.serializeNode(original)
        val deserialized = XlrDeserializer.parseNode(json)
        assertEquals(original, deserialized)
    }

    @Test
    fun `round-trip FunctionType with parameter defaults`() {
        val original =
            FunctionType(
                parameters =
                    listOf(
                        FunctionParameter(
                            name = "x",
                            type = StringType(),
                            default = StringType(const = "hi"),
                        ),
                        FunctionParameter(
                            name = "y",
                            type = NumberType(),
                            optional = true,
                        ),
                    ),
                returnType = BooleanType(),
            )
        val json = XlrSerializer.serializeNode(original)
        val deserialized = XlrDeserializer.parseNode(json)
        assertEquals(original, deserialized)
    }

    @Test
    fun `round-trip NumberType integer const preserves value`() {
        val original = NumberType(const = 42.0)
        val json = XlrSerializer.serializeNode(original)
        val deserialized = assertIs<NumberType>(XlrDeserializer.parseNode(json))
        assertEquals(42.0, deserialized.const)
    }

    @Test
    fun `round-trip RefType with property`() {
        val original = RefType(ref = "Foo", property = "bar")
        val json = XlrSerializer.serializeNode(original)
        val deserialized = XlrDeserializer.parseNode(json)
        assertEquals(original, deserialized)
    }

    @Test
    fun `round-trip StringType with enum`() {
        val original = StringType(enum = listOf("a", "b", "c"))
        val json = XlrSerializer.serializeNode(original)
        val deserialized = XlrDeserializer.parseNode(json)
        assertEquals(original, deserialized)
    }

    @Test
    fun `AdditionalItemsType None serializes to false`() {
        val obj = ObjectType(additionalProperties = AdditionalItemsType.None)
        val json = XlrSerializer.serializeNode(obj)
        assertEquals(JsonPrimitive(false), json["additionalProperties"])
    }

    @Test
    fun `TupleType additionalItems None serializes to false`() {
        val tuple =
            TupleType(
                elementTypes = listOf(TupleMember(type = StringType())),
                minItems = 1,
                additionalItems = AdditionalItemsType.None,
            )
        val json = XlrSerializer.serializeNode(tuple)
        assertEquals(JsonPrimitive(false), json["additionalItems"])
    }

    @Test
    fun `serialize returns parseable JSON string`() {
        val doc = XlrDocument(name = "Test", source = "test.ts", objectType = ObjectType())
        val jsonString = XlrSerializer.serialize(doc)
        val parsed =
            Json.parseToJsonElement(jsonString)
        assertIs<JsonObject>(parsed)
    }

    @Test
    fun `serialize round-trip via string form`() {
        val doc =
            XlrDocument(
                name = "Test",
                source = "test.ts",
                objectType =
                    ObjectType(
                        properties = mapOf("x" to ObjectProperty(required = true, node = StringType())),
                    ),
            )
        val jsonString = XlrSerializer.serialize(doc)
        val doc2 = XlrDeserializer.deserialize(jsonString)
        assertEquals(doc.name, doc2.name)
        assertEquals(doc.source, doc2.source)
        assertEquals(doc.objectType.properties, doc2.objectType.properties)
    }

    @Test
    fun `serializeDocument omits genericTokens when null`() {
        val doc = XlrDocument(name = "Test", source = "test.ts", objectType = ObjectType())
        val jsonObj = XlrSerializer.serializeDocument(doc)
        assertFalse(jsonObj.containsKey("genericTokens"))
    }

    @Test
    fun `serializeDocument includes genericTokens when present`() {
        val doc =
            XlrDocument(
                name = "Test",
                source = "test.ts",
                objectType = ObjectType(),
                genericTokens = listOf(ParamTypeNode(symbol = "T")),
            )
        val jsonObj = XlrSerializer.serializeDocument(doc)
        assertTrue(jsonObj.containsKey("genericTokens"))
    }

    @Test
    fun `serializeNode preserves JsonElement annotation fields`() {
        val examples =
            Json.parseToJsonElement("""["a", "b"]""")
        val defaultVal =
            Json.parseToJsonElement(""""hello"""")
        val see =
            Json.parseToJsonElement("""{"ref": "OtherType"}""")
        val node = StringType(examples = examples, default = defaultVal, see = see)
        val jsonObj = XlrSerializer.serializeNode(node)
        assertEquals(examples, jsonObj["examples"])
        assertEquals(defaultVal, jsonObj["default"])
        assertEquals(see, jsonObj["see"])
    }

    @Test
    fun `round-trip all 19 node types`() {
        for (original in TestFixtures.allNodeTypeInstances) {
            val json = XlrSerializer.serializeNode(original)
            val deserialized = XlrDeserializer.parseNode(json)
            assertEquals(original, deserialized, "Round-trip failed for ${original::class.simpleName}")
        }
    }

    @Test
    fun `serializeDocument flattens ObjectType at root`() {
        val doc =
            XlrDocument(
                name = "Test",
                source = "test.ts",
                objectType =
                    ObjectType(
                        properties = mapOf("x" to ObjectProperty(required = true, node = StringType())),
                    ),
            )
        val jsonObj = XlrSerializer.serializeDocument(doc)
        assertEquals("object", jsonObj["type"]?.jsonPrimitive?.content)
        assertFalse(jsonObj.containsKey("objectType"))
        assertTrue(jsonObj.containsKey("properties"))
    }

    @Test
    fun `serializeDocument name and source override ObjectType values`() {
        val obj = ObjectType(name = "InnerName", source = "inner.ts")
        val doc = XlrDocument(name = "OuterName", source = "outer.ts", objectType = obj)
        val jsonObj = XlrSerializer.serializeDocument(doc)
        assertEquals("OuterName", jsonObj["name"]?.jsonPrimitive?.content)
        assertEquals("outer.ts", jsonObj["source"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializeDocument preserves annotations on ObjectType root`() {
        val doc =
            XlrDocument(
                name = "Test",
                source = "test.ts",
                objectType =
                    ObjectType(
                        title = "TestTitle",
                        description = "A test object",
                        comment = "Some comment",
                    ),
            )
        val jsonObj = XlrSerializer.serializeDocument(doc)
        assertEquals("TestTitle", jsonObj["title"]?.jsonPrimitive?.content)
        assertEquals("A test object", jsonObj["description"]?.jsonPrimitive?.content)
        assertEquals("Some comment", jsonObj["comment"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serialize round-trip with genericTokens`() {
        val doc =
            XlrDocument(
                name = "Test",
                source = "test.ts",
                objectType = ObjectType(),
                genericTokens =
                    listOf(
                        ParamTypeNode(
                            symbol = "T",
                            constraints = RefType(ref = "Asset"),
                            default = RefType(ref = "Asset"),
                        ),
                    ),
            )
        val jsonString = XlrSerializer.serialize(doc)
        val doc2 = XlrDeserializer.deserialize(jsonString)
        assertEquals(doc.name, doc2.name)
        assertEquals(doc.source, doc2.source)
        assertNotNull(doc2.genericTokens)
        assertEquals(1, doc2.genericTokens!!.size)
        assertEquals("T", doc2.genericTokens!!.first().symbol)
    }
}
