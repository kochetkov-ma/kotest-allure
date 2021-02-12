package ru.iopump.kotest

import io.kotest.core.listeners.ProjectListener
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.AutoScan
import io.kotest.core.test.Description
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestStatus
import io.qameta.allure.Allure
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.model.ExecutableItem
import io.qameta.allure.model.Status
import io.qameta.allure.model.StatusDetails
import io.qameta.allure.util.ResultsUtils.createFrameworkLabel
import io.qameta.allure.util.ResultsUtils.createHostLabel
import io.qameta.allure.util.ResultsUtils.createLanguageLabel
import io.qameta.allure.util.ResultsUtils.createPackageLabel
import io.qameta.allure.util.ResultsUtils.createSuiteLabel
import io.qameta.allure.util.ResultsUtils.createThreadLabel
import org.opentest4j.TestAbortedException
import org.slf4j.LoggerFactory.getLogger
import ru.iopump.kotest.helper.AllureTestCase
import ru.iopump.kotest.helper.AllureTestCaseProcessor
import ru.iopump.kotest.helper.TestCaseMap
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.System.getProperty
import java.lang.System.setProperty
import java.nio.file.Paths
import io.qameta.allure.model.StepResult as AllureStepResult
import io.qameta.allure.model.TestResult as AllureTestCaseResult

const val ALLURE_RESULTS_DIR = "allure.results.directory"
const val CLEAR_ALLURE_RESULTS_DIR = "allure.results.directory.clear"
const val SKIP_ON_FAIL = "skip.on.fail"
const val ALLURE_SLF4J_LOG = "allure.slf4j.log"

@Suppress("unused")
@AutoScan
object IoPumpAllureListener : TestListener, ProjectListener {

    init {
        if (getProperty(ALLURE_RESULTS_DIR).isNullOrBlank())
            setProperty(ALLURE_RESULTS_DIR, "./build/allure-results")

        if (getProperty(CLEAR_ALLURE_RESULTS_DIR, false.toString()).toBoolean())
            Paths.get(getProperty(ALLURE_RESULTS_DIR, "./build/allure-results")).toFile().deleteRecursively()

        if (getProperty(ALLURE_SLF4J_LOG, true.toString()).toBoolean())
            Allure.setLifecycle(LoggedAllureLifecycle())
    }

    override val name: String = "IoPumpAllureListener"
    private val log = getLogger(IoPumpAllureListener::class.java)
    private val skipOnFail = getProperty(SKIP_ON_FAIL, true.toString())!!.toBoolean()

    private val usedGlobalNames = mutableMapOf<String, Int>()
    private val rootTcFailMap = mutableMapOf<String, TestResult>()
    private val testCaseMap = TestCaseMap()

    override suspend fun beforeTest(testCase: TestCase) {
        log.debug("[ALLURE] beforeTest ${testCase.description.name}")

        val processor = AllureTestCaseProcessor(testCase)
        val allureTestCase = testCase.map().put(testCase)
        when {
            allureTestCase.isRoot -> startRootTestCase(allureTestCase, processor)

            allureTestCase.refToNestedParent != null -> {
                startNestedTestCase(allureTestCase.refToNestedParent, allureTestCase, processor)
            }

            allureTestCase.refToRoot != null -> {
                val rootAllureTestCase = allureTestCase.refToRoot
                if (allureTestCase.isNewIteration) startRootTestCase(rootAllureTestCase, processor)
                processor.allLinks().toList().let { links ->
                    if (links.isNotEmpty())
                        allure().updateTestCase(rootAllureTestCase.uuid) {
                            it.links = (it.links + links).distinctBy { l -> l.url }
                        }
                }
                startNestedTestCase(rootAllureTestCase, allureTestCase, processor)
            }

            else -> log.error("[ALLURE] Internal error. Cannot get refToRoot in nested test. Skip this test case")
        }
        if (allureTestCase.refToRoot != null) checkAssume(allureTestCase.refToRoot.uuid)
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        val resultInfo = listOfNotNull(result.status, result.reason, result.error)
                .map { it.toString() }
                .filterNot { it.isBlank() }.joinToString(" - ")
        log.debug("[ALLURE] afterTest ${testCase.description.name} ($resultInfo)")

        val status = when (result.status) {
            TestStatus.Error -> Status.BROKEN
            TestStatus.Failure -> Status.FAILED
            TestStatus.Ignored -> Status.SKIPPED
            TestStatus.Success -> Status.PASSED
        }

        val details = StatusDetails().apply {
            message = result.error?.message
            if (result.error != null) trace = readStackTrace(result.error)
        }

        if (testCase.description.isRootTest()) {
            val rootTestCaseList = testCase.map().getRoot(testCase)
            val shift = if (rootTestCaseList.size >= 2) 1 else 0

            rootTestCaseList.forEachIndexed { index, tc ->
                val desc = tc.description(index + shift)
                val globalName = globalName(desc.name.displayName)

                allure().updateTestCase(tc.uuid) {
                    it.updateSpec(desc, globalName)
                    it.updateStatus(status, details)
                }

                allure().stopTestCase(tc.uuid)
                allure().writeTestCase(tc.uuid)
            }
        } else {
            val tc = testCase.map().getNested(testCase)

            if (tc.refToRoot != null && result.status != TestStatus.Success) {
                rootTcFailMap[tc.refToRoot.uuid] = result
                allure().updateTestCase(tc.refToRoot.uuid) { it.updateStatus(status, details) }
            }
            if (tc.refToNestedParent != null) allure().updateStep(tc.refToNestedParent.uuid) {
                it.updateStatus(
                        status,
                        details
                )
            }

            allure().updateStep(tc.uuid) { it.updateStatus(status, details) }
            allure().stopStep(tc.uuid)
        }
    }

    //// PRIVATE ////
    private fun checkAssume(uuid: String) {
        val result = rootTcFailMap[uuid]
        if ((result?.status ?: TestStatus.Success) != TestStatus.Success && skipOnFail) {
            val err = if (result?.error is TestAbortedException) result.error?.cause else result?.error
            throw TestAbortedException("One of the nested test has failed. Current scenario will be skipped", err)
        }
    }

    @Suppress("DEPRECATION")
    private fun AllureTestCaseResult.updateSpec(desc: Description, globalName: String) {
        this.fullName = desc.testDisplayPath().value
        this.name = globalName
        this.testCaseId = safeId(desc)
        this.historyId = globalName
    }

    @Suppress("DEPRECATION")
    private fun ExecutableItem.updateStatus(status: Status, details: StatusDetails) {
        val needUpdate = this.status == null
                || this.status == Status.PASSED
                || this.status == Status.SKIPPED
        if (needUpdate) {
            this.status = status
            this.statusDetails = details
        }
    }

    private fun startNestedTestCase(parent: AllureTestCase, case: AllureTestCase, prc: AllureTestCaseProcessor) {
        AllureStepResult().apply {
            description = case.testCase.description.testDisplayPath().value
            name = case.testCase.displayName
            description = prc.allDescriptions()
        }.let {
            allure().startStep(parent.uuid, case.uuid, it)
        }
    }

    private fun startRootTestCase(rootAllureTestCase: AllureTestCase, prc: AllureTestCaseProcessor) {
        val rootKotestCase = rootAllureTestCase.testCase

        val tcLabels = listOfNotNull(
                createSuiteLabel(rootKotestCase.description.spec().name.displayName),
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

    private fun safeId(description: Description): String =
        description.testId.value.replace('/', ' ').replace("[^\\sa-zA-Z0-9]".toRegex(), "")

    private fun TestCase.map() = testCaseMap

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

    private fun readStackTrace(throwable: Throwable?): String {
        val stringWriter = StringWriter()
        PrintWriter(stringWriter).use { printWriter -> throwable?.printStackTrace(printWriter) }
        return stringWriter.toString()
    }
}
