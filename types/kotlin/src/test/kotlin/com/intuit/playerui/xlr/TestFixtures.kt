package com.intuit.playerui.xlr

object TestFixtures {
    val choiceAssetJson: String by lazy {
        TestFixtures::class.java
            .getResourceAsStream("/test.json")!!
            .bufferedReader()
            .readText()
    }
}
