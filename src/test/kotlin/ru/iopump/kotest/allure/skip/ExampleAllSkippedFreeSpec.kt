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
class ExampleAllSkippedFreeSpec : FreeSpec() {

    init {

        "!Scenario: 1" - {
            forAll(
                row("--1--"),
                row("--2--")
            ) {
                "!Step 1 - $it" {
                    step1()
                }
                "!Step 2 - $it" {
                    stepNested()
                }
                "!Step 3 - $it" {
                    step2()
                    if (it == "--2--") throw AssertionError("Only on --2-- iteration")
                }
            }
        }

        "!Scenario: 2" - {
            forAll(
                row("--1--"),
                row("--2--"),
                row("--3--")
            ) {
                "Step 1 [$it]" {
                    step1()
                }
                "Step 2 [$it]" {
                    stepNested()
                }
                "Step 3 [$it]" {
                    step2()
                }
            }

            forAll(
                row("10"),
                row("20"),
            ) {
                "Step 4 [$it]" {
                    step1()
                    attachText("forAll")
                }
            }
        }
    }
}