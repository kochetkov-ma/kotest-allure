package ru.iopump.kotest.allure

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.checkAll
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import ru.iopump.kotest.allure.api.KotestAllureExecution.allureId
import ru.iopump.kotest.allure.api.KotestAllureExecution.task
import ru.iopump.kotest.allure.api.KotestAllureExecution.tms

@Epic("Allure feature annotation on test class")
@Feature("Data Driven")
class ExamplePropertySpec : FreeSpec() {

    init {
        "Property".tms("T-100").task("J-100").allureId("000") - {
            Arb.boolean().checkAll(2) {
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