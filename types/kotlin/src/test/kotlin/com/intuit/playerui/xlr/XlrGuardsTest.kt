package com.intuit.playerui.xlr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XlrGuardsTest {
    @Test
    fun `isPrimitiveType returns true for all primitive types`() {
        val primitives = TestFixtures.allNodeTypeInstances.filter { isPrimitiveType(it) }
        for (node in primitives) {
            assertTrue(isPrimitiveType(node), "Expected true for ${node::class.simpleName}")
        }
    }

    @Test
    fun `isPrimitiveType returns false for non-primitive types`() {
        val nonPrimitives = TestFixtures.allNodeTypeInstances.filter { !isPrimitiveType(it) }
        for (node in nonPrimitives) {
            assertFalse(isPrimitiveType(node), "Expected false for ${node::class.simpleName}")
        }
    }

    @Test
    fun `isAssetWrapperRef positive`() {
        assertTrue(isAssetWrapperRef(RefType(ref = "AssetWrapper<Foo>")))
        assertTrue(isAssetWrapperRef(RefType(ref = "AssetWrapper")))
    }

    @Test
    fun `isAssetWrapperRef negative`() {
        assertFalse(isAssetWrapperRef(RefType(ref = "Asset<Foo>")))
        assertFalse(isAssetWrapperRef(StringType()))
    }

    @Test
    fun `isAssetRef positive`() {
        assertTrue(isAssetRef(RefType(ref = "Asset<\"choice\">")))
        assertTrue(isAssetRef(RefType(ref = "Asset")))
    }

    @Test
    fun `isAssetRef negative`() {
        assertFalse(isAssetRef(RefType(ref = "AssetWrapper")))
        assertFalse(isAssetRef(StringType()))
    }

    @Test
    fun `isBindingRef positive`() {
        assertTrue(isBindingRef(RefType(ref = "Binding")))
        assertTrue(isBindingRef(RefType(ref = "Binding<string>")))
    }

    @Test
    fun `isBindingRef negative`() {
        assertFalse(isBindingRef(RefType(ref = "Expression")))
        assertFalse(isBindingRef(NumberType()))
    }

    @Test
    fun `isExpressionRef positive`() {
        assertTrue(isExpressionRef(RefType(ref = "Expression")))
        assertTrue(isExpressionRef(RefType(ref = "Expression<string>")))
    }

    @Test
    fun `isExpressionRef negative`() {
        assertFalse(isExpressionRef(RefType(ref = "Binding")))
        assertFalse(isExpressionRef(BooleanType()))
    }

    @Test
    fun `extractAssetTypeConstant extracts const from generic arg`() {
        val ref =
            RefType(
                ref = "Asset<\"choice\">",
                genericArguments = listOf(StringType(const = "choice")),
            )
        assertEquals("choice", extractAssetTypeConstant(ref))
    }

    @Test
    fun `extractAssetTypeConstant returns null for non-Asset ref`() {
        val ref = RefType(ref = "Binding")
        assertNull(extractAssetTypeConstant(ref))
    }

    @Test
    fun `extractAssetTypeConstant returns null for null ref`() {
        assertNull(extractAssetTypeConstant(null))
    }

    @Test
    fun `extractAssetTypeConstant returns null for empty genericArguments`() {
        val ref = RefType(ref = "Asset<\"choice\">", genericArguments = emptyList())
        assertNull(extractAssetTypeConstant(ref))
    }

    @Test
    fun `extractAssetTypeConstant returns null for non-string generic arg`() {
        val ref =
            RefType(
                ref = "Asset<\"choice\">",
                genericArguments = listOf(NumberType()),
            )
        assertNull(extractAssetTypeConstant(ref))
    }

    @Test
    fun `hasConstValue returns true when const is set`() {
        assertTrue(hasConstValue(StringType(const = "hello")))
    }

    @Test
    fun `hasConstValue returns false when const is null`() {
        assertFalse(hasConstValue(StringType()))
    }

    @Test
    fun `hasAnyConstValue returns true for string with const`() {
        assertTrue(hasAnyConstValue(StringType(const = "x")))
    }

    @Test
    fun `hasAnyConstValue returns true for number with const`() {
        assertTrue(hasAnyConstValue(NumberType(const = 42.0)))
    }

    @Test
    fun `hasAnyConstValue returns true for boolean with const`() {
        assertTrue(hasAnyConstValue(BooleanType(const = true)))
    }

    @Test
    fun `hasAnyConstValue returns false for types without const`() {
        assertFalse(hasAnyConstValue(StringType()))
        assertFalse(hasAnyConstValue(ObjectType(properties = emptyMap())))
        assertFalse(hasAnyConstValue(NullType()))
    }

    @Test
    fun `isLiteralUnion returns true when all members have const`() {
        val union =
            OrType(
                orTypes =
                    listOf(
                        StringType(const = "a"),
                        StringType(const = "b"),
                        NumberType(const = 1.0),
                    ),
            )
        assertTrue(isLiteralUnion(union))
    }

    @Test
    fun `isLiteralUnion returns false when any member lacks const`() {
        val union =
            OrType(
                orTypes =
                    listOf(
                        StringType(const = "a"),
                        StringType(),
                    ),
            )
        assertFalse(isLiteralUnion(union))
    }

    @Test
    fun `getLiteralValues extracts all const values`() {
        val union =
            OrType(
                orTypes =
                    listOf(
                        StringType(const = "a"),
                        NumberType(const = 2.0),
                        BooleanType(const = false),
                    ),
            )
        assertEquals(listOf("a", 2.0, false), getLiteralValues(union))
    }

    @Test
    fun `getLiteralValues skips members without const`() {
        val union =
            OrType(
                orTypes =
                    listOf(
                        StringType(const = "a"),
                        NullType(),
                        NumberType(const = 3.0),
                    ),
            )
        assertEquals(listOf("a", 3.0), getLiteralValues(union))
    }

    @Test
    fun `isAssetRef returns false for similar but non-matching ref`() {
        assertFalse(isAssetRef(RefType(ref = "Assets")))
    }

    @Test
    fun `isBindingRef returns false for prefix-only match`() {
        assertFalse(isBindingRef(RefType(ref = "BindingFoo")))
    }

    @Test
    fun `isExpressionRef returns false for prefix-only match`() {
        assertFalse(isExpressionRef(RefType(ref = "ExpressionFoo")))
    }

    @Test
    fun `isLiteralUnion returns true for empty OrType`() {
        assertTrue(isLiteralUnion(OrType(orTypes = emptyList())))
    }

    @Test
    fun `getLiteralValues returns empty list for empty OrType`() {
        assertEquals(emptyList<Any>(), getLiteralValues(OrType(orTypes = emptyList())))
    }

    @Test
    fun `extractAssetTypeConstant returns null when genericArguments is null`() {
        val ref = RefType(ref = "Asset<\"choice\">")
        assertNull(extractAssetTypeConstant(ref))
    }

    @Test
    fun `extractAssetTypeConstant returns null for StringType without const`() {
        val ref =
            RefType(
                ref = "Asset<\"choice\">",
                genericArguments = listOf(StringType()),
            )
        assertNull(extractAssetTypeConstant(ref))
    }
}
