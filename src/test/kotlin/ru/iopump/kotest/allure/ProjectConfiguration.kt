package ru.iopump.kotest.allure

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import ru.iopump.kotest.allure.api.KotestAllureExecution.EXECUTION_START_CALLBACK
import ru.iopump.kotest.allure.api.KotestAllureExecution.setUpFixture

object ProjectConfiguration : AbstractProjectConfig() {
    override val parallelism: Int = 2

    init {
        EXECUTION_START_CALLBACK = { it.setUpFixture("Project Set Up") }
    }

    override fun extensions(): List<Extension> = listOf(
        KotestAllureListener
    )
}