package com.intuit.playerui.xlr

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class XlrTypesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `StringType defaults`() {
        val s = StringType()
        assertEquals("string", s.type)
        assertNull(s.const)
        assertNull(s.enum)
        assertNull(s.title)
        assertNull(s.description)
        assertNull(s.name)
        assertNull(s.examples)
        assertNull(s.default)
        assertNull(s.see)
        assertNull(s.comment)
        assertNull(s.meta)
        assertNull(s.source)
        assertNull(s.genericTokens)
    }

    @Test
    fun `NumberType defaults`() {
        val n = NumberType()
        assertEquals("number", n.type)
        assertNull(n.const)
        assertNull(n.enum)
    }

    @Test
    fun `BooleanType defaults`() {
        val b = BooleanType()
        assertEquals("boolean", b.type)
        assertNull(b.const)
    }

    @Test
    fun `all simple types have correct type field`() {
        assertEquals("null", NullType().type)
        assertEquals("any", AnyType().type)
        assertEquals("unknown", UnknownType().type)
        assertEquals("undefined", UndefinedType().type)
        assertEquals("void", VoidType().type)
        assertEquals("never", NeverType().type)
    }

    @Test
    fun `RefType construction`() {
        val ref = RefType(ref = "Foo", genericArguments = listOf(StringType()))
        assertEquals("ref", ref.type)
        assertEquals("Foo", ref.ref)
        assertEquals(1, ref.genericArguments?.size)
    }

    @Test
    fun `ObjectType defaults`() {
        val obj = ObjectType()
        assertEquals("object", obj.type)
        assertEquals(emptyMap(), obj.properties)
        assertNull(obj.extends)
        assertEquals(AdditionalItemsType.None, obj.additionalProperties)
    }

    @Test
    fun `ObjectType with source and genericTokens`() {
        val tokens = listOf(ParamTypeNode(symbol = "T", constraints = RefType(ref = "Asset")))
        val obj = ObjectType(source = "test.ts", genericTokens = tokens)
        assertEquals("test.ts", obj.source)
        assertNotNull(obj.genericTokens)
        assertEquals(1, obj.genericTokens!!.size)
        assertEquals("T", obj.genericTokens!!.first().symbol)
    }

    @Test
    fun `AdditionalItemsType None and Typed`() {
        val none = AdditionalItemsType.None
        val typed = AdditionalItemsType.Typed(StringType())
        assertIs<AdditionalItemsType.None>(none)
        assertIs<AdditionalItemsType.Typed>(typed)
        assertIs<StringType>(typed.node)
    }

    @Test
    fun `ConditionalCheck construction`() {
        val check = ConditionalCheck(left = StringType(), right = NumberType())
        assertIs<StringType>(check.left)
        assertIs<NumberType>(check.right)
    }

    @Test
    fun `ConditionalValue construction`() {
        val value = ConditionalValue(trueValue = BooleanType(), falseValue = NullType())
        assertIs<BooleanType>(value.trueValue)
        assertIs<NullType>(value.falseValue)
    }

    @Test
    fun `ConditionalType construction`() {
        val ct =
            ConditionalType(
                check = ConditionalCheck(StringType(), NumberType()),
                value = ConditionalValue(BooleanType(), NullType()),
            )
        assertEquals("conditional", ct.type)
    }

    @Test
    fun `NamedType construction`() {
        val nt = NamedType(typeName = "Foo", source = "bar.ts", node = StringType(const = "x"))
        assertEquals("Foo", nt.typeName)
        assertEquals("bar.ts", nt.source)
        assertNull(nt.genericTokens)
        assertIs<StringType>(nt.node)
        assertEquals("x", (nt.node as StringType).const)
    }

    @Test
    fun `NamedTypeWithGenerics construction`() {
        val token = ParamTypeNode(symbol = "T", constraints = RefType(ref = "Asset"))
        val ntg =
            NamedTypeWithGenerics(
                typeName = "Foo",
                source = "bar.ts",
                genericTokens = listOf(token),
                node = ObjectType(),
            )
        assertEquals("Foo", ntg.typeName)
        assertEquals(1, ntg.genericTokens.size)
        assertEquals("T", ntg.genericTokens.first().symbol)
    }

    @Test
    fun `NodeTypeWithGenerics construction`() {
        val ntwg =
            NodeTypeWithGenerics(
                node = ArrayType(elementType = StringType()),
                genericTokens = listOf(ParamTypeNode(symbol = "T")),
            )
        assertIs<ArrayType>(ntwg.node)
        assertEquals(1, ntwg.genericTokens.size)
    }

    @Test
    fun `XlrDocument toObjectType delegates`() {
        val objType =
            ObjectType(
                properties = mapOf("x" to ObjectProperty(required = true, node = StringType())),
                title = "Test",
            )
        val doc = XlrDocument(name = "Test", source = "test.ts", objectType = objType)
        assertEquals(objType, doc.toObjectType())
    }

    @Test
    fun `TupleType construction`() {
        val tt =
            TupleType(
                elementTypes =
                    listOf(
                        TupleMember(name = "a", type = StringType()),
                        TupleMember(type = NumberType(), optional = true),
                    ),
                minItems = 1,
                additionalItems = AdditionalItemsType.Typed(AnyType()),
            )
        assertEquals("tuple", tt.type)
        assertEquals(2, tt.elementTypes.size)
        assertEquals(1, tt.minItems)
        assertIs<AdditionalItemsType.Typed>(tt.additionalItems)
    }

    @Test
    fun `FunctionType construction`() {
        val ft =
            FunctionType(
                parameters =
                    listOf(
                        FunctionParameter(name = "x", type = StringType(), optional = true),
                    ),
                returnType = BooleanType(),
            )
        assertEquals("function", ft.type)
        assertEquals(1, ft.parameters.size)
    }

    @Test
    fun `polymorphic serialization round-trip via NodeType serializer`() {
        val original: NodeType = StringType(const = "test", title = "MyTitle")
        val serialized = json.encodeToString(NodeType.serializer(), original)
        val deserialized = json.decodeFromString(NodeType.serializer(), serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `NodeType sealed hierarchy includes all 19 types`() {
        val instances = TestFixtures.allNodeTypeInstances
        assertEquals(19, instances.size)
        for (instance in instances) {
            assertNotNull(instance.type)
        }
    }

    @Test
    fun `Annotated interface is implemented by all NodeTypes`() {
        val node: NodeType = StringType(title = "hello", comment = "world")
        val annotated: Annotated = node
        assertEquals("hello", annotated.title)
        assertEquals("world", annotated.comment)
    }

    @Test
    fun `ParamTypeNode construction`() {
        val param = ParamTypeNode(symbol = "T", constraints = RefType(ref = "Foo"), default = StringType())
        assertEquals("T", param.symbol)
        assertIs<RefType>(param.constraints)
        assertIs<StringType>(param.default)
    }

    @Test
    fun `ObjectProperty construction`() {
        val prop = ObjectProperty(required = true, node = NumberType(const = 42.0))
        assertEquals(true, prop.required)
        assertIs<NumberType>(prop.node)
    }

    @Test
    fun `RecordType defaults`() {
        val r = RecordType(keyType = StringType(), valueType = AnyType())
        assertEquals("record", r.type)
        assertNull(r.name)
        assertNull(r.title)
        assertNull(r.description)
        assertNull(r.source)
        assertNull(r.genericTokens)
    }

    @Test
    fun `ArrayType defaults`() {
        val a = ArrayType(elementType = StringType())
        assertEquals("array", a.type)
        assertNull(a.name)
        assertNull(a.source)
        assertNull(a.genericTokens)
    }

    @Test
    fun `TemplateLiteralType defaults`() {
        val t = TemplateLiteralType(format = ".*")
        assertEquals("template", t.type)
        assertNull(t.name)
        assertNull(t.source)
        assertNull(t.genericTokens)
    }

    @Test
    fun `OrType defaults`() {
        val o = OrType(orTypes = listOf(StringType()))
        assertEquals("or", o.type)
        assertNull(o.name)
        assertNull(o.source)
        assertNull(o.genericTokens)
    }

    @Test
    fun `AndType defaults`() {
        val a = AndType(andTypes = listOf(StringType()))
        assertEquals("and", a.type)
        assertNull(a.name)
        assertNull(a.source)
        assertNull(a.genericTokens)
    }

    @Test
    fun `RefType defaults`() {
        val r = RefType(ref = "Foo")
        assertEquals("ref", r.type)
        assertNull(r.genericArguments)
        assertNull(r.property)
        assertNull(r.name)
        assertNull(r.source)
        assertNull(r.genericTokens)
    }

    @Test
    fun `FunctionType defaults`() {
        val f = FunctionType(parameters = emptyList())
        assertEquals("function", f.type)
        assertNull(f.returnType)
        assertNull(f.name)
        assertNull(f.source)
        assertNull(f.genericTokens)
    }

    @Test
    fun `FunctionParameter with all fields`() {
        val p =
            FunctionParameter(
                name = "x",
                type = StringType(),
                optional = true,
                default = StringType(const = "hi"),
            )
        assertEquals("x", p.name)
        assertIs<StringType>(p.type)
        assertEquals(true, p.optional)
        val defaultNode = assertIs<StringType>(p.default)
        assertEquals("hi", defaultNode.const)
    }

    @Test
    fun `TupleMember defaults`() {
        val m = TupleMember(type = StringType())
        assertNull(m.name)
        assertNull(m.optional)
        assertIs<StringType>(m.type)
    }

    @Test
    fun `XlrDocument defaults`() {
        val doc = XlrDocument(name = "Test", source = "test.ts", objectType = ObjectType())
        assertEquals("Test", doc.name)
        assertEquals("test.ts", doc.source)
        assertNull(doc.genericTokens)
    }

    @Test
    fun `data class equality for identical instances`() {
        val a = StringType(const = "hello", title = "Title")
        val b = StringType(const = "hello", title = "Title")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `data class inequality for different field values`() {
        val a = StringType(const = "hello")
        val b = StringType(const = "world")
        assertNotEquals(a, b)
    }

    @Test
    fun `data class copy with modified fields`() {
        val original = StringType(const = "hello", title = "T")
        val copied = original.copy(const = "world")
        assertEquals("world", copied.const)
        assertEquals("T", copied.title)
    }

    @Test
    fun `StringType with JsonElement annotations`() {
        val examples = Json.parseToJsonElement("""["a", "b"]""")
        val defaultVal = Json.parseToJsonElement(""""hello"""")
        val see = Json.parseToJsonElement("""{"ref": "OtherType"}""")
        val s = StringType(examples = examples, default = defaultVal, see = see)
        assertIs<JsonArray>(s.examples)
        assertIs<JsonPrimitive>(s.default)
        assertIs<JsonObject>(s.see)
    }

    @Test
    fun `WholeNumberDoubleSerializer serializes negative whole number as integer`() {
        val element = json.encodeToJsonElement(NodeType.serializer(), NumberType(const = -42.0))
        assertEquals(JsonPrimitive(-42), element.jsonObject["const"])
    }

    @Test
    fun `WholeNumberDoubleSerializer serializes zero as integer`() {
        val element = json.encodeToJsonElement(NodeType.serializer(), NumberType(const = 0.0))
        assertEquals(JsonPrimitive(0), element.jsonObject["const"])
    }

    @Test
    fun `WholeNumberDoubleSerializer serializes large whole number as integer`() {
        val element = json.encodeToJsonElement(NodeType.serializer(), NumberType(const = 1000000.0))
        assertEquals(JsonPrimitive(1000000), element.jsonObject["const"])
    }

    @Test
    fun `AdditionalItemsTypeSerializer encodes None to false`() {
        val encoded = json.encodeToJsonElement(AdditionalItemsTypeSerializer, AdditionalItemsType.None)
        assertEquals(JsonPrimitive(false), encoded)
    }

    @Test
    fun `AdditionalItemsTypeSerializer encodes Typed to node object`() {
        val encoded =
            json.encodeToJsonElement(
                AdditionalItemsTypeSerializer,
                AdditionalItemsType.Typed(StringType()),
            )
        assertIs<JsonObject>(encoded)
        assertEquals("string", encoded.jsonObject["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `AdditionalItemsTypeSerializer decodes false to None`() {
        val decoded = json.decodeFromJsonElement(AdditionalItemsTypeSerializer, JsonPrimitive(false))
        assertIs<AdditionalItemsType.None>(decoded)
    }

    @Test
    fun `AdditionalItemsTypeSerializer decodes JsonNull to None`() {
        val decoded = json.decodeFromJsonElement(AdditionalItemsTypeSerializer, JsonNull)
        assertIs<AdditionalItemsType.None>(decoded)
    }

    @Test
    fun `AdditionalItemsTypeSerializer decodes node object to Typed`() {
        val nodeJson = json.parseToJsonElement("""{"type": "string"}""")
        val decoded = json.decodeFromJsonElement(AdditionalItemsTypeSerializer, nodeJson)
        val typed = assertIs<AdditionalItemsType.Typed>(decoded)
        assertIs<StringType>(typed.node)
    }

    @Test
    fun `AdditionalItemsTypeSerializer round-trips None`() {
        val original = AdditionalItemsType.None
        val encoded = json.encodeToJsonElement(AdditionalItemsTypeSerializer, original)
        val decoded = json.decodeFromJsonElement(AdditionalItemsTypeSerializer, encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `AdditionalItemsTypeSerializer round-trips Typed`() {
        val original = AdditionalItemsType.Typed(StringType(const = "test"))
        val encoded = json.encodeToJsonElement(AdditionalItemsTypeSerializer, original)
        val decoded = json.decodeFromJsonElement(AdditionalItemsTypeSerializer, encoded)
        assertEquals(original, decoded)
    }
}
