package com.intuit.playerui.xlr

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class XlrTypesTest {
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
    fun `xlrSerializersModule is a valid SerializersModule`() {
        assertIs<SerializersModule>(xlrSerializersModule)
    }

    @Test
    fun `polymorphic serialization round-trip via Json`() {
        val jsonInstance =
            Json {
                serializersModule = xlrSerializersModule
                ignoreUnknownKeys = true
                classDiscriminator = "_type"
            }
        val original = StringType(const = "test", title = "MyTitle")
        val serialized = jsonInstance.encodeToString(kotlinx.serialization.serializer<StringType>(), original)
        val deserialized = jsonInstance.decodeFromString(kotlinx.serialization.serializer<StringType>(), serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `NodeType sealed hierarchy includes all 19 types`() {
        val instances: List<NodeType> =
            listOf(
                StringType(),
                NumberType(),
                BooleanType(),
                NullType(),
                AnyType(),
                UnknownType(),
                UndefinedType(),
                VoidType(),
                NeverType(),
                RefType(ref = "Foo"),
                ObjectType(),
                ArrayType(elementType = StringType()),
                TupleType(elementTypes = emptyList(), minItems = 0),
                RecordType(keyType = StringType(), valueType = AnyType()),
                OrType(orTypes = emptyList()),
                AndType(andTypes = emptyList()),
                TemplateLiteralType(format = ".*"),
                ConditionalType(
                    check = ConditionalCheck(StringType(), NumberType()),
                    value = ConditionalValue(BooleanType(), NullType()),
                ),
                FunctionType(parameters = emptyList()),
            )
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
}
