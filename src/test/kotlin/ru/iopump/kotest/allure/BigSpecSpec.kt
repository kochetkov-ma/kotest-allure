package ru.iopump.kotest.allure

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.positiveInts
import io.qameta.allure.Epic
import io.qameta.allure.Feature

@Epic("Allure feature annotation on test class")
@Feature("Concurrency")
class BigSpecSpec : FreeSpec() {

    override fun concurrency(): Int = 2

    init {
        "Scenario: Getting employee by id" - {

            var expectedId = 0
            "Given test environment is up and test data prepared" {
                expectedId = Arb.positiveInts().next()
            }

            "When client sent request to get the employee by id=$expectedId" { }

            "Then client received response with status 200 and id=$expectedId" { }
        }

        "Scenario: Creating new employee" - {

            "Given test environment is up and test data prepared" { }

            "When client sent request to create new employee" { }

            "Then server received request with employee" { }

            "And client received response with status 200 with generated id" { }
        }
    }
}