package ru.iopump.kotest.allure.helper

import io.kotest.core.test.Description
import io.kotest.core.test.TestResult.Companion.success
import io.kotest.core.test.TestStatus
import io.qameta.allure.model.StepResult
import io.qameta.allure.model.TestResultContainer
import ru.iopump.kotest.allure.KotestAllureListener.log
import ru.iopump.kotest.allure.api.KotestAllureConstant.VAR.DATA_DRIVEN_SUPPORT
import ru.iopump.kotest.allure.api.KotestAllureExecution.ALLURE
import ru.iopump.kotest.allure.api.KotestAllureExecution.containerUuid
import ru.iopump.kotest.allure.helper.InternalExecutionModel.Iteration.Factory.scenario
import ru.iopump.kotest.allure.helper.InternalUtil.containerUuidWithIteration
import ru.iopump.kotest.allure.helper.InternalUtil.displayNameWithIterationSuffix
import ru.iopump.kotest.allure.helper.InternalUtil.processSkipResult
import ru.iopump.kotest.allure.helper.InternalUtil.prop
import ru.iopump.kotest.allure.helper.InternalUtil.toAllure
import ru.iopump.kotest.allure.helper.InternalUtil.toOptional
import ru.iopump.kotest.allure.helper.InternalUtil.updateStatus
import ru.iopump.kotest.allure.helper.InternalUtil.updateStepResult
import ru.iopump.kotest.allure.helper.InternalUtil.updateTestResult
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object InternalExecutionModel {
    private val dataDrivenSupport: Boolean = DATA_DRIVEN_SUPPORT.prop(true)
    private val testUuidMap: MutableMap<Description, String> = ConcurrentHashMap()
    private val iterationMap: MutableMap<Description, Iteration> = ConcurrentHashMap()

    internal fun Description.currentIterationIndex(): Int = iterationMap[this]?.index ?: 0

    private data class Iteration(val index: Int, val scenario: KotestTestCase, var startLineNumber: Int) {
        val isNotStarted get() = startLineNumber <= 0
        fun start(step: KotestTestCase) {
            startLineNumber = step.source.lineNumber
        }

        companion object Factory {
            fun scenario(scenario: KotestTestCase, previous: Iteration?) =
                Iteration(previous?.index?.inc() ?: 0, scenario, 0)
        }
    }

    internal fun startScenario(testCase: KotestTestCase): String =
        testUuidMap.computeIfAbsent(testCase.description) { uuid() }
            .also { uuid ->
                val index = when (dataDrivenSupport) {
                    true -> iterationMap
                        .compute(testCase.description) { _, iteration -> scenario(testCase, iteration) }
                        ?.index
                        ?.also { if (it >= 1) log.debug("New iteration has been started with index '$it'") }
                        ?: 0
                    false -> 0
                }
                val metadata = AllureMetadata(testCase.spec::class, testCase.description)
                val result = AllureTestResult().apply { updateTestResult(uuid, testCase, metadata, index) }
                TestResultContainer().also { containerResult ->
                    containerResult.uuid = testCase.description.containerUuidWithIteration(index)
                    containerResult.name = testCase.description.displayNameWithIterationSuffix(index)
                    ALLURE.startTestContainer(testCase.spec.containerUuid, containerResult)
                    ALLURE.scheduleTestCase(containerResult.uuid, result)
                    ALLURE.startTestCase(uuid)
                }
            }

    internal fun stopScenario(testCase: KotestTestCase, testResult: KotestTestResult, prune: Boolean = true) {
        testUuidMap[testCase.description].toOptional().ifPresentOrElse(
            { uuid ->
                ALLURE.updateTestCase(uuid) { it.updateStatus(testResult.toAllure()) }
                ALLURE.stopTestCase(uuid)
                ALLURE.writeTestCase(uuid)
                val containerUuid = testCase.description.containerUuid
                ALLURE.stopTestContainer(containerUuid)
                ALLURE.writeTestContainer(containerUuid)

                if (dataDrivenSupport && prune) iterationMap.remove(testCase.description)
                testUuidMap.remove(testCase.description)
            },
            { log.error("Cannot stop Scenario '$testCase' because it hasn't been started") }
        )
    }

    /**
     * Private helper for [startStep]
     */
    private fun processSkip(testCase: KotestTestCase) {
        testCase.scenario.toOptional().ifPresent { scenario ->
            testUuidMap[scenario].toOptional().ifPresent { scenarioUuid ->
                ALLURE.updateTestCase(scenarioUuid) {
                    processSkipResult(it)
                }
            }
        }
    }

    /**
     * Private helper for [startStep]
     */
    private fun processIteration(testCase: KotestTestCase) =
        testCase.scenario.toOptional().ifPresent { scenario ->
            iterationMap[scenario].toOptional().ifPresent { iteration ->
                if (iteration.isNotStarted) {
                    iteration.start(testCase)
                } else {
                    if (testCase.isNewIteration(iteration)) {
                        stopScenario(iteration.scenario, success(0), false)
                        startScenario(iteration.scenario)
                        iteration.start(testCase)
                    }
                }
            }
        }

    internal fun startStep(testCase: KotestTestCase) {
        testUuidMap.computeIfAbsent(testCase.description) { uuid() }
            .also { uuid ->
                if (dataDrivenSupport) processIteration(testCase)
                testCase.parentUuid.toOptional()
                    .ifPresentOrElse(
                        { parentUuid ->
                            val metadata = AllureMetadata(description = testCase.description)
                            val result = StepResult().also { it.updateStepResult(testCase, metadata) }
                            ALLURE.startStep(parentUuid, uuid, result)
                            processSkip(testCase)
                        },
                        { startScenario(testCase) }
                    )
            }
    }

    internal fun stopStep(testCase: KotestTestCase, testResult: KotestTestResult) {
        testUuidMap[testCase.description].toOptional().ifPresentOrElse(
            { uuid ->
                testCase.parentUuid.toOptional().ifPresentOrElse(
                    {
                        ALLURE.updateStep(uuid) { it.updateStatus(testResult.toAllure()) }
                        ALLURE.stopStep(uuid)
                        if (testResult.needPassOnTop) {
                            testCase.description.parent.parents().forEach { description ->
                                val updateFunction = { parentUuid: String ->
                                    if (description.isRootTest())
                                        ALLURE.updateTestCase(parentUuid) { it.updateStatus(testResult.toAllure()) }
                                    else
                                        ALLURE.updateStep(parentUuid) { it.updateStatus(testResult.toAllure()) }
                                }
                                testUuidMap[description].toOptional().ifPresent(updateFunction)
                            }
                        }
                        testUuidMap.remove(testCase.description)
                    },
                    { stopScenario(testCase, testResult) }
                )
            },
            { log.error("Cannot stop Step '$testCase' because it hasn't been started") }
        )
    }

    /////////////////
    //// PRIVATE ////
    /////////////////

    private fun uuid(): String = UUID.randomUUID().toString()

    private fun KotestTestCase.isNewIteration(iteration: Iteration): Boolean =
        source.lineNumber <= iteration.startLineNumber

    private val KotestTestCase.scenario: Description? get() = description.parent.parents().firstOrNull()

    private val KotestTestCase.parentUuid: String? get() = testUuidMap[description.parent]

    private val KotestTestResult.needPassOnTop: Boolean get() = status in arrayOf(TestStatus.Error, TestStatus.Failure)
}