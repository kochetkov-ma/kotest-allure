package ru.iopump.kotest.allure.fast

import io.kotest.core.spec.style.FreeSpec
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story

@Epic("Allure feature annotation on test class")
@Feature("FreeSpec")
@Story("Fail Fast")
class ExampleFailFastFreeSpec : FreeSpec() {

    init {
        failfast = true

        "Scenario: should be skipped steps after fail" - {

            "Step: passed 1" {}

            "Step: failed 2" { error("Fail Fast ERROR") }

            "Step: skipped 3" {}

            "Step: skipped 4" {}
        }
    }
}