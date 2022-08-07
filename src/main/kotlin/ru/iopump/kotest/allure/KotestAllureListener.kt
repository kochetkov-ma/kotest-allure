package ru.iopump.kotest.allure

import io.kotest.core.config.ProjectConfiguration
import io.kotest.core.descriptors.Descriptor.TestDescriptor
import io.kotest.core.descriptors.DescriptorId
import io.kotest.core.descriptors.toDescriptor
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.listeners.IgnoredSpecListener
import io.kotest.core.listeners.InstantiationErrorListener
import io.kotest.core.listeners.ProjectListener
import io.kotest.core.listeners.TestListener
import io.kotest.core.names.TestName
import io.kotest.core.spec.AutoScan
import io.kotest.core.spec.DslDrivenSpec
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestType
import io.kotest.engine.test.status.isEnabled
import io.qameta.allure.model.TestResultContainer
import ru.iopump.kotest.allure.api.KotestAllureExecution.ALLURE
import ru.iopump.kotest.allure.api.KotestAllureExecution.EXECUTION_START_CALLBACK
import ru.iopump.kotest.allure.api.KotestAllureExecution.PROJECT_UUID
import ru.iopump.kotest.allure.api.KotestAllureExecution.containerUuid
import ru.iopump.kotest.allure.helper.InternalExecutionModel.startScenario
import ru.iopump.kotest.allure.helper.InternalExecutionModel.startStep
import ru.iopump.kotest.allure.helper.InternalExecutionModel.stopScenario
import ru.iopump.kotest.allure.helper.InternalExecutionModel.stopStep
import ru.iopump.kotest.allure.helper.InternalUtil.logger
import ru.iopump.kotest.allure.helper.KotestTestCase
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

/**
 * Extended Kotest Allure listener.
 * It provides full hierarchy model in Allure Report instead of official Kotest extension.
 * With unlimited nesting of steps and support for iterations.
 * See [ru.iopump.kotest.allure.api.KotestAllureExecution] - get access to Spec and Project uuid to add Allure fixture.
 * See
 */
@AutoScan
object KotestAllureListener : ProjectListener, TestListener, InstantiationErrorListener, TestCaseExtension,
    IgnoredSpecListener {
    private val projectConfigurationDefault = ProjectConfiguration()
    internal val log = logger<KotestAllureListener>()

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
        if (testCase.descriptor.isRootTest()) startScenario(testCase)
        else startStep(testCase)
    }

    override suspend fun afterContainer(testCase: TestCase, result: TestResult) {
        debug("afterContainer - $testCase - $result")
        if (testCase.descriptor.isRootTest()) stopScenario(testCase, result)
        else stopStep(testCase, result)
    }

    override suspend fun beforeEach(testCase: TestCase) {
        debug("beforeEach - $testCase")
        if (testCase.descriptor.isRootTest()) startScenario(testCase)
        else startStep(testCase)
    }

    override suspend fun beforeTest(testCase: TestCase) {
        if (testCase.type == TestType.Dynamic)
            beforeEach(testCase)
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        debug("afterEach - $testCase - $result")
        if (testCase.descriptor.isRootTest()) stopScenario(testCase, result)
        else stopStep(testCase, result)
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        if (testCase.type == TestType.Dynamic)
            afterEach(testCase, result)
    }

    /**
     * Process skipped TestCase/Test Step.
     */
    override suspend fun intercept(testCase: TestCase, execute: suspend (TestCase) -> TestResult): TestResult {
        val skipped = testCase.isEnabled(projectConfigurationDefault).isEnabled.not()

        if (skipped) {
            debug("intercept on skipped test - $testCase")
            beforeEach(testCase)
        }
        val executionResult = execute(testCase)

        if (skipped) afterEach(testCase, executionResult)

        return executionResult
    }

    override suspend fun ignoredSpec(kclass: KClass<*>, reason: String?) {
        debug("specIgnored - $kclass")

        emptySpecInternal(kclass, kclass.toDescriptor().id.value, TestResult.Ignored(reason))
    }

    /**
     * On spec class creation error.
     * For example spring context errors.
     */
    override suspend fun instantiationError(kclass: KClass<*>, t: Throwable) {
        debug("instantiationError - $kclass - ${t.localizedMessage}")
        emptySpecInternal(kclass, kclass.toDescriptor().id.value, TestResult.Error(0.seconds, t))
    }

    private fun emptySpecInternal(kclass: KClass<*>, message: String, testResult: TestResult) {
        val specContainerUuid = kclass.containerUuid
        val specContainerResult = TestResultContainer().apply {
            uuid = specContainerUuid
            name = specContainerUuid
        }
        ALLURE.startTestContainer(PROJECT_UUID, specContainerResult)
        val informationTestCase = KotestTestCase(
            descriptor = TestDescriptor(kclass.toDescriptor(), DescriptorId(message)),
            name = TestName(null, message, null, false),
            spec = object : DslDrivenSpec() {},
            test = {},
            type = TestType.Test
        )

        startScenario(informationTestCase)
        stopScenario(informationTestCase, testResult)

        ALLURE.stopTestContainer(specContainerUuid)
        ALLURE.writeTestContainer(specContainerUuid)
    }

    /////////////////
    //// PRIVATE ////
    /////////////////

    //private suspend fun TestCase.isSkipped() = this.isFocused()ski isEnabled().isEnabled.not()

    private fun debug(msg: String) = if (log.isDebugEnabled) log.debug(msg) else null
}