package ru.iopump.kotest.allure

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.qameta.allure.Allure
import io.qameta.allure.Epic
import io.qameta.allure.Link
import io.qameta.allure.Step
import ru.iopump.kotest.allure.annotation.KDescription
import ru.iopump.kotest.allure.annotation.KTag
import ru.iopump.kotest.allure.api.KotestAllureExecution.setUpFixture
import ru.iopump.kotest.allure.api.KotestAllureExecution.tearDownFixture

@Epic("Listener contract")
@KTag("contract-fixture")
@KDescription("Listener contract fixture description")
@Link(name = "fixture documentation", url = "https://example.org/listener-contract")
class AllureListenerFixtureSpec : FreeSpec() {

    init {
        setUpFixture("contract spec setup")

        "fixture passed [FIX-10] (CASE-20) #42" - {
            "fixture outer step" - {
                "fixture inner step" {
                    Allure.addAttachment("fixture attachment", "text/plain", "fixture attachment content")
                }
            }
        }

        "fixture caught expected exception" {
            shouldThrow<IllegalStateException> { expectedExceptionStep() }
        }

        "fixture data scenario" - {
            forAll(row("alpha"), row("beta")) { value ->
                "fixture data $value" { Allure.step("native data step $value") }
            }
        }

        "!fixture skipped scenario" { error("disabled fixture executed") }

        "fixture failed assertion" - {
            "fixture failing child" { throw AssertionError("fixture deliberate assertion failure") }
        }

        "fixture broken exception" { throw IllegalStateException("fixture deliberate unexpected error") }

        tearDownFixture("contract spec teardown")
    }

    @Step("expected exception annotation step")
    private fun expectedExceptionStep(): Unit = throw IllegalStateException("fixture expected exception")
}
