package ru.iopump.kotest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.qameta.allure.*
import ru.iopump.kotest.annotation.KDescription
import ru.iopump.kotest.annotation.KJira
import ru.iopump.kotest.annotation.KJiras

@Epic("Allure feature annotation on test class")
@Feature("Behavior")
@Links(
    value = [Link(url = "http://iopump.ru"), Link(url = "https://ya.ru")]
)
@KDescription(
    """
    This is multiline description.
    It must be a new line
"""
)
@Issues(
        value = [Issue("TTT-666"), Issue("TTT-777")]
)
@KJiras(
        value = [KJira("TTT-111"), KJira("TTT-000")]
)
class ExampleBddSpec : BehaviorSpec() {

    init {
        Given("[PRJ-100] Start kotest specification Scenario") {
            forAll(row("FirstIterArg"), row("SecondIterArg")) { arg ->

                When("Start step 1 [PRJ-110] - $arg") {
                    step1()
                }
                Then("[PRJ-160] Nested step has been printed - $arg") {
                    stepException1()
                }
                And("Step 2 has been printed too [PRJ-1300] - $arg") {
                    stepException2()
                }
            }
        }
    }
}