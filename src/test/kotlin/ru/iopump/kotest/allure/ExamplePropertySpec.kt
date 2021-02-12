package ru.iopump.kotest.allure

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bool
import io.kotest.property.checkAll
import io.qameta.allure.Epic
import io.qameta.allure.Feature


@Epic("Allure feature annotation on test class")
@Feature("Data Driven")
class ExamplePropertySpec : FreeSpec() {

    init {
        "Property" - {
            Arb.bool().checkAll(2) {
                val index = attempts()
                "Nested Scenario $index" - {
                    "Step $index" {
                        stepNested()
                    }
                }
            }
        }
    }
}