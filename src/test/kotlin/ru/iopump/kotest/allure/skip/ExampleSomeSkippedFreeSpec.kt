package ru.iopump.kotest.allure.skip

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Links
import ru.iopump.kotest.allure.attachText
import ru.iopump.kotest.allure.step1
import ru.iopump.kotest.allure.step2
import ru.iopump.kotest.allure.stepNested

@Epic("Allure feature annotation on test class")
@Feature("FreeSpec")
@Links(
    value = [
        Link("iopump.ru"),
        Link("ya.ru")
    ]
)
class ExampleSomeSkippedFreeSpec : FreeSpec() {

    init {

        "Start kotest specification Scenario 1 (skip test)" - {
            forAll(
                row("--1--"),
                row("--2--")
            ) {
                "Start step 1 - $it" {
                    step1()
                }
                "!Skipped step  - $it" {
                    stepNested()
                }
                "Step 2 has been printed too  - $it" {
                    step2()
                    if (it == "--2--") throw AssertionError("Only on --2-- iteration")
                }
            }
        }

        "!Skipped Scenario 2" - {
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
                "!Just single step [$it]" {
                    step1()
                    attachText("forAll")
                }
            }
        }
    }
}