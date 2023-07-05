package ru.iopump.kotest.allure

import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Links
import ru.iopump.kotest.allure.api.KotestAllureExecution.setUpFixture
import ru.iopump.kotest.allure.api.KotestAllureExecution.tearDownFixture

@Epic("Allure feature annotation on test class")
@Feature("StringSpec")
@Links(
    value = [
        Link("iopump.ru"),
        Link("ya.ru")
    ]
)
class ExampleStringSpec : StringSpec() {

    init {
        setUpFixture("Set up testing fixture SPEC ExampleStringSpec")

        "Start kotest specification Scenario 2" {
            forAll(row("--1--"), row("--2--")) {
                step1()
                step2()
                if (it == "--1--") {
                    testCase.tearDownFixture("TearDown fixture for ERROR in integration $it")
                    stepException1()
                }
            }
        }

        tearDownFixture("Tear Down testing fixture  SPEC ExampleStringSpec")
    }
}