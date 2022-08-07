package ru.iopump.kotest.allure.disabled

import io.kotest.core.spec.style.FreeSpec
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.junit.jupiter.api.Disabled

@Epic("Allure feature annotation on test class")
@Feature("FreeSpec")
@Story("@Disabled")
@Disabled("should be disabled by junit")
class ExampleDisabledJunitFreeSpec : FreeSpec() {

    init {
        "Scenario: should be disabled by junit" - {
            "Step: should be disabled by junit" { error("should be disabled by junit but not") }
        }
    }
}