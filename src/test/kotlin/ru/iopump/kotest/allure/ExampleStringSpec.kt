package ru.iopump.kotest.allure

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Links
import io.qameta.allure.Step
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
        setUpFixture("Set up testing fixture")

        "Start kotest specification Scenario 2 #666" {
            forAll(row("--1--"), row("--2--")) {
                step1()
                step2()
                if (it == "--1--") {
                    stepException1()
                }
            }
        }

        "If step throws exception in shouldThrow block test must not break" {
            shouldThrow<Exception> { throwException() }
        }

        tearDownFixture("Tear Down testing fixture")
    }

    @Step("Throw exception")
    fun throwException() {
        throw Exception()
    }
}