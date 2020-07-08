package ru.iopump.kotest

import io.kotest.core.listeners.ProjectListener
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.AutoScan
import io.kotest.core.spec.Spec
import io.kotest.core.test.Description
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestStatus
import io.qameta.allure.Allure
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.model.Status
import io.qameta.allure.model.StatusDetails
import io.qameta.allure.util.ResultsUtils.*
import org.junit.platform.commons.util.ExceptionUtils
import org.slf4j.LoggerFactory.getLogger
import ru.iopump.kotest.helper.AllureTestCase
import ru.iopump.kotest.helper.AllureTestCaseProcessor
import ru.iopump.kotest.helper.TestCaseMap
import java.nio.file.Paths
import io.qameta.allure.model.StepResult as AllureStepResult
import io.qameta.allure.model.TestResult as AllureTestCaseResult

const val ALLURE_RESULTS_DIR = "allure.results.directory"
const val CLEAR_ALLURE_RESULTS_DIR = "allure.results.directory.clear"

@Suppress("unused")
@AutoScan
object IoPumpAllureListener : TestListener, ProjectListener {
    var fixResultDir = true
    private val log = getLogger(IoPumpAllureListener::class.java)
    private val usedGlobalNames = mutableMapOf<String, Int>()
    private val testCaseMap = mutableMapOf<Spec, TestCaseMap>()

    override val name = "IoPumpAllureTestListener"

    private fun globalName(name: String): String {
        val globalIndex = usedGlobalNames.computeIfPresent(name) { _, v -> v + 1 } ?: usedGlobalNames.put(name, 0) ?: 0
        return if (globalIndex >= 1) "$name ($globalIndex)" else name
    }

    private fun allure(): AllureLifecycle = try {
        Allure.getLifecycle() ?: throw IllegalStateException()
    } catch (t: Throwable) {
        log.error("Error getting allure lifecycle", t)
        throw t
    }

    override suspend fun beforeProject() {
        if (fixResultDir && System.getProperty(ALLURE_RESULTS_DIR) == null) {
            System.setProperty(ALLURE_RESULTS_DIR, "./build/allure-results")
        }
        if (System.getProperty(CLEAR_ALLURE_RESULTS_DIR, "true")!!.toBoolean()) {
            Paths.get(System.getProperty(ALLURE_RESULTS_DIR, "./allure-results")).toFile().deleteRecursively()
        }
    }

    private fun safeId(description: Description): String =
        description.id().replace('/', ' ').replace("[^\\sa-zA-Z0-9]".toRegex(), "")

    override suspend fun beforeSpec(spec: Spec) {
        testCaseMap[spec] = TestCaseMap()
    }

    override suspend fun afterSpec(spec: Spec) {
        testCaseMap.remove(spec)
    }

    private fun TestCase.map() = testCaseMap[this.spec]!!

    override suspend fun beforeTest(testCase: TestCase) {
        log.debug("Allure beforeTest $testCase")
        val processor = AllureTestCaseProcessor(testCase)

        val allureTestCase = testCase.map().put(testCase)

        when {
            allureTestCase.isRoot -> startRootTestCase(allureTestCase, processor)

            allureTestCase.refToRoot != null -> {
                val rootAllureTestCase = allureTestCase.refToRoot
                if (allureTestCase.isNewIteration) startRootTestCase(rootAllureTestCase, processor)

                processor.allLinks().toList().let { links ->
                    if (links.isNotEmpty())
                        allure().updateTestCase(rootAllureTestCase.uuid) {
                            it.links = (it.links + links).distinctBy { l -> l.url }
                        }
                }

                AllureStepResult().apply {
                    description = testCase.description.fullName()
                    name = testCase.description.name.displayName()
                    description = processor.allDescriptions()
                }.let {
                    allure().startStep(rootAllureTestCase.uuid, allureTestCase.uuid, it)
                }
            }

            else -> {
                log.error("IoPumpAllureListener internal error. Cannot get refToRoot in nested test. Skip this test case")
            }
        }
    }

    private fun startRootTestCase(rootAllureTestCase: AllureTestCase, prc: AllureTestCaseProcessor) {
        val rootKotestCase = rootAllureTestCase.testCase

        val tcLabels = listOfNotNull(
            createSuiteLabel(rootKotestCase.description.spec().name.displayName()),
            createThreadLabel(),
            createHostLabel(),
            createLanguageLabel("kotlin"),
            createFrameworkLabel("kotest"),
            createPackageLabel(rootKotestCase.spec::class.java.`package`.name),
            prc.epic(),
            prc.story(),
            prc.feature(),
            prc.severity(),
            prc.owner()
        )

        AllureTestCaseResult().apply {
            uuid = rootAllureTestCase.uuid
            labels = tcLabels
            links = prc.allLinks().toMutableList()
            description = prc.allDescriptions()
        }.let {
            allure().scheduleTestCase(it)
            allure().startTestCase(rootAllureTestCase.uuid)
        }
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        log.debug("Allure afterTest $testCase")
        val status = when (result.status) {
            TestStatus.Error -> Status.BROKEN
            TestStatus.Failure -> Status.FAILED
            TestStatus.Ignored -> Status.SKIPPED
            TestStatus.Success -> Status.PASSED
        }

        val details = StatusDetails()
        details.message = result.error?.message
        if (result.error != null) details.trace = ExceptionUtils.readStackTrace(result.error)

        if (testCase.isTopLevel()) {
            val rootTestCaseList = testCase.map().getRoot(testCase)
            val shift = if (rootTestCaseList.size >= 2) 1 else 0

            rootTestCaseList.forEachIndexed { index, tc ->
                val desc = tc.description(index + shift)
                val globalName = globalName(desc.name.displayName())
                allure().updateTestCase(tc.uuid) {
                    it.fullName = desc.fullName()
                    it.name = globalName
                    it.testCaseId = safeId(desc)
                    it.historyId = globalName

                    if (status != Status.PASSED || it.status == null) it.status = status
                    if (details.message != null || it.statusDetails == null) it.statusDetails = details
                }
                allure().stopTestCase(tc.uuid)
                allure().writeTestCase(tc.uuid)
            }
        } else {
            val tc = testCase.map().getNested(testCase)

            if (status != Status.PASSED && tc.refToRoot != null) {
                allure().updateTestCase(tc.refToRoot.uuid) {
                    it.status = status
                    it.statusDetails = details
                }
            }

            allure().updateStep(tc.uuid) {
                it.status = status
                it.statusDetails = details
            }
            allure().stopStep(tc.uuid)
        }
    }
}
