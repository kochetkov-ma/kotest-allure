package ru.iopump.kotest.allure.disabled

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FreeSpec
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story

@Epic("Allure feature annotation on test class")
@Feature("FreeSpec")
@Story("@Ignored")
@Ignored
class ExampleIgnoredFreeSpec : FreeSpec() {

    init {
        "Scenario: should be ignored" - {
            "Step: should be ignored" { error("should be ignored but not") }
        }
    }
}