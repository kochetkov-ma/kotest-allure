package ru.iopump.kotest.allure.helper

import io.kotest.core.descriptors.Descriptor
import io.kotest.core.descriptors.Descriptor.TestDescriptor
import io.kotest.core.source.SourceRef
import io.kotest.core.source.SourceRef.ClassSource
import io.kotest.core.source.SourceRef.FileSource
import io.kotest.core.source.SourceRef.None
import io.kotest.core.test.TestResult.Success
import io.qameta.allure.model.StepResult
import ru.iopump.kotest.allure.KotestAllureListener.log
import ru.iopump.kotest.allure.api.KotestAllureConstant.VAR.DATA_DRIVEN_SUPPORT
import ru.iopump.kotest.allure.api.KotestAllureExecution.ALLURE
import ru.iopump.kotest.allure.api.KotestAllureExecution.containerUuid
import ru.iopump.kotest.allure.helper.InternalExecutionModel.Iteration.Factory.scenario
import ru.iopump.kotest.allure.helper.InternalUtil.processSkipResult
import ru.iopump.kotest.allure.helper.InternalUtil.prop
import ru.iopump.kotest.allure.helper.InternalUtil.toAllure
import ru.iopump.kotest.allure.helper.InternalUtil.toOptional
import ru.iopump.kotest.allure.helper.InternalUtil.updateStatus
import ru.iopump.kotest.allure.helper.InternalUtil.updateStepResult
import ru.iopump.kotest.allure.helper.InternalUtil.updateTestResult
import ru.iopump.kotest.allure.helper.meta.AllureMetadata
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

object InternalExecutionModel {
    private val dataDrivenSupport: Boolean = DATA_DRIVEN_SUPPORT.prop(true)
    private val testUuidMap: MutableMap<Descriptor, String> = ConcurrentHashMap()
    private val iterationMap: MutableMap<Descriptor, Iteration> = ConcurrentHashMap()

    private data class Iteration(val index: Int, val scenario: KotestTestCase, var startLineNumber: Int) {
        val isNotStarted get() = startLineNumber <= 0
        fun start(step: KotestTestCase) {
            startLineNumber = step.source.lineNumber()
        }

        companion object Factory {
            fun scenario(scenario: KotestTestCase, previous: Iteration?) =
                Iteration(previous?.index?.inc() ?: 0, scenario, 0)
        }
    }

    internal fun startScenario(testCase: KotestTestCase): String =
        testUuidMap.computeIfAbsent(testCase.descriptor) { uuid() }
            .also { uuid ->
                val index = when (dataDrivenSupport) {
                    true -> iterationMap
                        .compute(testCase.descriptor) { _, iteration -> scenario(testCase, iteration) }
                        ?.index
                        ?.also { if (it >= 1) log.debug("New iteration has been started with index '$it'") }
                        ?: 0
                    false -> 0
                }
                val metadata = AllureMetadata(testCase.spec::class, testCase.descriptor)
                val result = AllureTestResult().apply { updateTestResult(uuid, testCase, metadata, index) }
                ALLURE.scheduleTestCase(testCase.spec.containerUuid, result)
                ALLURE.startTestCase(uuid)
            }

    internal fun stopScenario(testCase: KotestTestCase, testResult: KotestTestResult, prune: Boolean = true) {
        testUuidMap[testCase.descriptor].toOptional().ifPresentOrElse(
            { uuid ->
                ALLURE.updateTestCase(uuid) { it.updateStatus(testResult.toAllure()) }
                ALLURE.stopTestCase(uuid)
                ALLURE.writeTestCase(uuid)

                if (dataDrivenSupport && prune) iterationMap.remove(testCase.descriptor)
                testUuidMap.remove(testCase.descriptor)
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
                        stopScenario(iteration.scenario, Success(0.milliseconds), false)
                        startScenario(iteration.scenario)
                        iteration.start(testCase)
                    }
                }
            }
        }

    internal fun startStep(testCase: KotestTestCase) {
        testUuidMap.computeIfAbsent(testCase.descriptor) { uuid() }
            .also { uuid ->
                if (dataDrivenSupport) processIteration(testCase)
                testCase.parentUuid.toOptional()
                    .ifPresentOrElse(
                        { parentUuid ->
                            val metadata = AllureMetadata(description = testCase.descriptor)
                            val result = StepResult().also { it.updateStepResult(testCase, metadata) }
                            ALLURE.startStep(parentUuid, uuid, result)
                            processSkip(testCase)
                        },
                        { startScenario(testCase) }
                    )
            }
    }

    internal fun stopStep(testCase: KotestTestCase, testResult: KotestTestResult) {
        testUuidMap[testCase.descriptor].toOptional().ifPresentOrElse(
            { uuid ->
                testCase.parentUuid.toOptional().ifPresentOrElse(
                    {
                        ALLURE.updateStep(uuid) { it.updateStatus(testResult.toAllure()) }
                        ALLURE.stopStep(uuid)
                        if (testResult.needPassOnTop) {
                            testCase.descriptor.parents().forEach { description ->
                                val updateFunction = { parentUuid: String ->
                                    if (description.isRootTest())
                                        ALLURE.updateTestCase(parentUuid) { it.updateStatus(testResult.toAllure()) }
                                    else
                                        ALLURE.updateStep(parentUuid) { it.updateStatus(testResult.toAllure()) }
                                }
                                testUuidMap[description].toOptional().ifPresent(updateFunction)
                            }
                        }
                        testUuidMap.remove(testCase.descriptor)
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

    private fun SourceRef.lineNumber() = when (this) {
        is None -> {
            System.err.println(
                "Cannot determine correct line number for DDT iteration! " +
                        "You should add test sources to your project. " +
                        "Or disable DATA_DRIVEN_SUPPORT > 'kotest.allure.data.driven=false'"
            )
            0
        }
        is FileSource -> lineNumber ?: 0
        is ClassSource -> lineNumber ?: 0
    }

    private fun uuid(): String = UUID.randomUUID().toString()

    private fun KotestTestCase.isNewIteration(iteration: Iteration): Boolean =
        source.lineNumber() <= iteration.startLineNumber

    private val KotestTestCase.scenario: Descriptor?
        get() =
            descriptor.parents().firstOrNull { it is TestDescriptor }

    private val KotestTestCase.parentUuid: String? get() = testUuidMap[descriptor.parent]

    private val KotestTestResult.needPassOnTop: Boolean get() = isErrorOrFailure
}