package ru.iopump.kotest.allure

import io.kotest.core.config.AbstractProjectConfig
import ru.iopump.kotest.allure.api.KotestAllureExecution.EXECUTION_START_CALLBACK
import ru.iopump.kotest.allure.api.KotestAllureExecution.setUpFixture

class ProjectConfiguration : AbstractProjectConfig() {
    override val extensions = listOf(KotestAllureListener)
    
    init {
        EXECUTION_START_CALLBACK = { it.setUpFixture("Project Set Up") }
    }
}