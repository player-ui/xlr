package com.intuit.playerui.xlr

import kotlinx.serialization.json.Json

internal val xlrJson =
    Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
        isLenient = true
    }
