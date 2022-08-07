package ru.iopump.kotest.allure

import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.qameta.allure.Epic
import io.qameta.allure.Feature

@Epic("Allure feature annotation on test class")
@Feature("Data Driven")
class ExampleDataDrivenSpec : FreeSpec() {

    init {
        "Scenario" - {
            withData(
                nameFn = { "Test with $it" },
                "one" to 1,
                "two" to 2
            ) {
                stepNested()
            }
        }
    }
}