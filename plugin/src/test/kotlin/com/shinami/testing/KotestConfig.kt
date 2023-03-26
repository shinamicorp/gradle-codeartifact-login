package com.shinami.testing

import io.kotest.core.config.AbstractProjectConfig

class KotestConfig : AbstractProjectConfig() {
    init {
        // For better display in gradle test output.
        // https://kotest.io/docs/framework/test_output.html
        displayFullTestPath = true
    }
}
