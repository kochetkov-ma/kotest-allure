package ru.iopump.kotest.allure

import io.kotest.core.listeners.ProjectListener
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.AutoScan
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.qameta.allure.model.TestResultContainer
import ru.iopump.kotest.allure.api.Execution.ALLURE
import ru.iopump.kotest.allure.api.Execution.EXECUTION_START_CALLBACK
import ru.iopump.kotest.allure.api.Execution.PROJECT_UUID
import ru.iopump.kotest.allure.api.Execution.containerUuid
import ru.iopump.kotest.allure.helper.InternalExecutionModel.startScenario
import ru.iopump.kotest.allure.helper.InternalExecutionModel.startStep
import ru.iopump.kotest.allure.helper.InternalExecutionModel.stopScenario
import ru.iopump.kotest.allure.helper.InternalExecutionModel.stopStep
import ru.iopump.kotest.allure.helper.InternalUtil.logger
import kotlin.reflect.KClass

/**
 * Extended Kotest Allure listener.
 * It provides full hierarchy model in Allure Report instead of official Kotest extension.
 * With unlimited nesting of steps and support for iterations.
 * See [ru.iopump.kotest.allure.api.Execution] - get access to Spec and Project uuid to add Allure fixture.
 * See
 */
@AutoScan
object KotestAllureListener : ProjectListener, TestListener {
    private val log = logger<KotestAllureListener>()
    override val name: String = "kotest_allure_listener"

    override suspend fun beforeProject() {
        debug("beforeProject")

        TestResultContainer().run {
            uuid = PROJECT_UUID
            name = "KOTEST PROJECT EXECUTION"
            ALLURE.startTestContainer(this)
            EXECUTION_START_CALLBACK(uuid)
        }
    }

    override suspend fun afterProject() {
        debug("afterProject")

        ALLURE.stopTestContainer(PROJECT_UUID)
        ALLURE.writeTestContainer(PROJECT_UUID)
    }

    override suspend fun prepareSpec(kclass: KClass<out Spec>) {
        debug("prepareSpec - $kclass")

        val specContainerUuid = kclass.containerUuid
        val specContainerResult = TestResultContainer().apply {
            uuid = specContainerUuid
            name = specContainerUuid
        }

        ALLURE.startTestContainer(PROJECT_UUID, specContainerResult)
    }


    override suspend fun finalizeSpec(kclass: KClass<out Spec>, results: Map<TestCase, TestResult>) {
        debug("finalizeSpec - $kclass")

        val specContainerUuid = kclass.containerUuid
        ALLURE.stopTestContainer(specContainerUuid)
        ALLURE.writeTestContainer(specContainerUuid)
    }

    override suspend fun beforeContainer(testCase: TestCase) {
        debug("beforeContainer - $testCase")
        if (testCase.description.isRootTest()) startScenario(testCase)
        else startStep(testCase)
    }

    override suspend fun afterContainer(testCase: TestCase, result: TestResult) {
        debug("afterContainer - $testCase - $result")
        if (testCase.description.isRootTest()) stopScenario(testCase, result)
        else stopStep(testCase, result)
    }

    override suspend fun beforeEach(testCase: TestCase) {
        debug("beforeEach - $testCase")
        if (testCase.description.isRootTest()) startScenario(testCase)
        else startStep(testCase)
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        debug("afterEach - $testCase - $result")
        if (testCase.description.isRootTest()) stopScenario(testCase, result)
        else stopStep(testCase, result)
    }

    /////////////////
    //// PRIVATE ////
    /////////////////

    private fun debug(msg: String) = if (log.isDebugEnabled) log.debug("ALLURE $msg") else null
}