package com.intuit.playerui.xlr

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    XlrDeserializerTest::class,
    XlrGuardsTest::class,
    XlrSerializerTest::class,
    XlrTypesTest::class,
)
class AllTests
