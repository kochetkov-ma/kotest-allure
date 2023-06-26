package ru.iopump.kotest.allure

import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FreeSpec
import io.qameta.allure.Epic
import io.qameta.allure.Feature


@Epic("Allure feature annotation on test class")
@Feature("Data Driven")
class ExampleDataDrivenSpec : FreeSpec() {

    init {
        "Scenario" - {
            forAll<Int>(
                "one" to 1,
                "two" to 2
            ) {

                "Nested Scenario $it" - {
                    "Step $it" {
                        stepNested()
                    }
                }
            }
        }
    }
}