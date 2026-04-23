package com.intuit.playerui.xlr

object TestFixtures {
    val choiceAssetJson: String by lazy {
        TestFixtures::class.java
            .getResourceAsStream("/test.json")!!
            .bufferedReader()
            .readText()
    }

    /** List of all 19 NodeType instances for cross-test reuse. */
    val allNodeTypeInstances: List<NodeType> by lazy {
        listOf(
            StringType(const = "x"),
            NumberType(const = 1.0),
            BooleanType(const = true),
            NullType(),
            AnyType(),
            UnknownType(),
            UndefinedType(),
            VoidType(),
            NeverType(),
            RefType(ref = "Foo"),
            ObjectType(properties = mapOf("a" to ObjectProperty(required = true, node = StringType()))),
            ArrayType(elementType = StringType()),
            TupleType(elementTypes = listOf(TupleMember(type = StringType())), minItems = 1),
            RecordType(keyType = StringType(), valueType = AnyType()),
            OrType(orTypes = listOf(StringType(), NumberType())),
            AndType(andTypes = listOf(StringType(), NumberType())),
            TemplateLiteralType(format = ".*"),
            ConditionalType(
                check = ConditionalCheck(StringType(), NumberType()),
                value = ConditionalValue(BooleanType(), NullType()),
            ),
            FunctionType(parameters = listOf(FunctionParameter(name = "x", type = StringType()))),
        )
    }
}
