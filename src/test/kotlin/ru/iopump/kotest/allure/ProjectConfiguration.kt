package ru.iopump.kotest.allure

import io.kotest.core.config.AbstractProjectConfig
import ru.iopump.kotest.allure.api.Execution.EXECUTION_START_CALLBACK
import ru.iopump.kotest.allure.api.Execution.setUpFixture

object ProjectConfiguration : AbstractProjectConfig() {
    init {
        EXECUTION_START_CALLBACK = { it.setUpFixture("Project Set Up") }
    }
}