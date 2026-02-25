package com.intuit.playerui.xlr

import org.junit.runner.RunWith
import org.junit.runners.Suite

// New test classes must be registered here; Bazel uses this suite as the entry point.
@RunWith(Suite::class)
@Suite.SuiteClasses(
    XlrDeserializerTest::class,
    XlrGuardsTest::class,
    XlrSerializerTest::class,
    XlrTypesTest::class,
    XlrUtilityTest::class,
)
class AllTests
