package ru.iopump.kotest.allure

import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import io.qameta.allure.TmsLinks
import ru.iopump.kotest.allure.annotation.KAllureId

@Epic("Allure feature annotation on test class")
@Feature("Data Driven")
@TmsLinks(value = [TmsLink("T-1"), TmsLink("T-2")])
@KAllureId(value = "1")
class ExampleDataDrivenSpec : FreeSpec() {

    init {
        "Scenario [J-100] (T-3) #999" - {
            withData(
                nameFn = { "Test with $it" },
                "one" to 1,
                "two" to 2
            ) {
                stepNested()
            }
        }
    }
}