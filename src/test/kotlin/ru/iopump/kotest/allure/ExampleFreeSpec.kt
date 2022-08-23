package ru.iopump.kotest.allure

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Links
import ru.iopump.kotest.allure.api.KotestAllureExecution.setUpFixture
import ru.iopump.kotest.allure.api.KotestAllureExecution.tearDownFixture

@Epic("Allure feature annotation on test class")
@Feature("FreeSpec")
@Links(
    value = [
        Link("iopump.ru"),
        Link("ya.ru")
    ]
)
class ExampleFreeSpec : FreeSpec() {

    init {

        setUpFixture("Set Up fixture for this SPEC ExampleFreeSpec")
        tearDownFixture("TearDown fixture for this SPEC ExampleFreeSpec")

        "Start kotest specification Scenario 1" - {


            forAll(
                row("--1--"),
                row("--2--")
            ) {
                "Start step 1 - $it" {
                    step1()
                }

                testCase.setUpFixture("Set Up fixture for this iteration $it")
                testCase.tearDownFixture("TearDown fixture for this iteration $it")

                "Nested step has been printed  - $it" {
                    stepNested()
                }
                "Step 2 has been printed too  - $it" {
                    step2()
                    if (it == "--2--") throw AssertionError("Only on --2-- iteration")
                }
            }
        }

        "Start kotest specification Scenario 2" - {
            forAll(
                row("--1--"),
                row("--2--"),
                row("--3--")
            ) {
                "Start step 1 [$it]" {
                    step1()
                }
                "Nested step has been printed [$it]" {
                    stepNested()
                }
                "Step 2 has been printed too [$it]" {
                    step2()
                }
            }

            forAll(
                row("10"),
                row("20"),
            ) {
                "Just single step [$it]" {
                    step1()
                    attachText("forAll")
                }
            }
        }

        "Start kotest specification Scenario 3" - {
            testCase.tearDownFixture("TearDown Start kotest specification Scenario 3")
            "STEP" { error("STEP") }
        }
    }
}