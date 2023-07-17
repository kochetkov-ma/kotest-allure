package ru.iopump.kotest.allure.helper

import io.kotest.core.test.Description
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestStatus
import io.qameta.allure.model.Label
import io.qameta.allure.model.Status
import io.qameta.allure.model.StatusDetails
import io.qameta.allure.util.ResultsUtils
import org.opentest4j.TestAbortedException
import org.slf4j.LoggerFactory
import ru.iopump.kotest.allure.api.KotestAllureConstant.VAR.SKIP_ON_FAIL
import ru.iopump.kotest.allure.api.KotestAllureExecution.bestName
import ru.iopump.kotest.allure.api.KotestAllureExecution.uuid
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.System.getProperty
import java.lang.System.getenv
import java.math.BigDecimal
import java.util.Optional
import kotlin.reflect.full.isSubclassOf

internal object InternalUtil {
    private val skipOnFail = SKIP_ON_FAIL.prop(true)

    private val AllureTestResult.isBad get() = status in arrayOf(Status.FAILED, Status.BROKEN)

    internal inline fun <reified T> T.toOptional() = Optional.ofNullable(this)

    internal fun String.logger() = LoggerFactory.getLogger(this)

    internal inline fun <reified T> logger() = LoggerFactory.getLogger(T::class.java)

    internal inline fun <reified T> String.prop(default: T): T = prop() ?: default

    internal inline fun <reified T> String.prop(): T? = (
            getProperty(this)
                ?: getProperty(toUpperCase())
                ?: getProperty(toLowerCase())
                ?: getenv(this)
                ?: getenv(toUpperCase())
                ?: getenv(toLowerCase())
                ?: getenv(replace(".", "_"))
                ?: getenv(replace(".", "_").toUpperCase())
                ?: getenv(replace(".", "_").toLowerCase())
            )
        .smartCast()

    internal inline fun <reified T> Any?.smartCast(): T? =
        when {
            this == null -> null
            this is T -> this
            T::class.isSubclassOf(Boolean::class) -> toString().toBoolean() as T
            T::class.isSubclassOf(BigDecimal::class) -> toString().toBigDecimalOrNull() as T
            T::class.isSubclassOf(Double::class) -> toString().toDoubleOrNull() as T
            T::class.isSubclassOf(Int::class) -> toString().toIntOrNull() as T
            T::class.isSubclassOf(Long::class) -> toString().toLongOrNull() as T
            else -> this as T
        }

    internal fun TestResult.toAllure(): Pair<Status, StatusDetails> {
        val status = when (this.status) {
            TestStatus.Error -> Status.BROKEN
            TestStatus.Failure -> Status.FAILED
            TestStatus.Ignored -> Status.SKIPPED
            TestStatus.Success -> Status.PASSED
        }

        val details = this.error?.let { throwable ->
            StatusDetails().apply {
                this.message = throwable.toString()
                this.trace = throwable.readStackTrace()
            }
        } ?: StatusDetails()

        return status to details
    }

    internal fun AllureTestResult.updateTestResult(
        testUuid: String,
        test: TestCase,
        meta: AllureMetadata,
        iteration: Int = 0
    ) {
        uuid = testUuid

        name = test.description.displayNameWithIterationSuffix(iteration)
        description = meta.allDescriptions

        fullName = test.description.bestName().withIterationSuffix(iteration)
        testCaseId = test.description.bestName().withIterationSuffix(iteration)
        historyId = test.description.bestName().withIterationSuffix(iteration)
        labels = testCaseLabels(test, meta)
        links = meta.allLinks
    }

    internal fun Description.displayNameWithIterationSuffix(iteration: Int = 0) =
        name.name + " [$iteration]".takeIf { iteration >= 1 }.orEmpty()

    private fun String.withIterationSuffix(iteration: Int = 0) =
        this + "$iteration".takeIf { iteration >= 1 }.orEmpty()

    internal fun Description.containerUuidWithIteration(iteration: Int = 0) =
        bestName().withIterationSuffix(iteration).uuid()

    internal fun AllureStepResult.updateStepResult(testCase: TestCase, metadata: AllureMetadata) {
        name = testCase.description.name.name
        description = metadata.allDescriptions
    }

    internal fun AllureTestResult.updateStatus(statusAndDetails: Pair<Status, StatusDetails>) {
        val currentStatus = this.status
        val needUpdate = currentStatus == null  // если еще нет статуса
                || currentStatus == Status.PASSED  // если текущий статус пройден
                || currentStatus == Status.SKIPPED // если статус пропущен
        if (needUpdate) {
            this.status = statusAndDetails.first
            this.statusDetails = statusAndDetails.second
        }
    }

    internal fun AllureStepResult.updateStatus(statusAndDetails: Pair<Status, StatusDetails>) {
        val currentStatus = this.status
        val needUpdate = currentStatus == null // если еще нет статуса
                || currentStatus == Status.PASSED // если текущий статус пройден
                || currentStatus == Status.SKIPPED // если статус пропущен
        if (needUpdate) {
            this.status = statusAndDetails.first
            this.statusDetails = statusAndDetails.second
        }
    }

    private const val skipMsg: String =
        "Previous step was failed. This will be skipped due to '$SKIP_ON_FAIL' option is true\n"

    internal fun processSkipResult(result: AllureTestResult) {
        if (skipOnFail and result.isBad) {
            val causeMessage = result.statusDetails.message.removePrefix(skipMsg)
            throw TestAbortedException(skipMsg + causeMessage)
        }
    }

    /////////////////
    //// PRIVATE ////
    /////////////////

    private fun Throwable.readStackTrace(): String {
        val stringWriter = StringWriter()
        PrintWriter(stringWriter).use { printWriter -> printStackTrace(printWriter) }
        return stringWriter.toString()
    }

    private fun testCaseLabels(testCase: TestCase, metadata: AllureMetadata): List<Label> {
        val pkgName = testCase.spec::class.java.`package`.name

        return listOfNotNull(
            ResultsUtils.createSuiteLabel(testCase.description.spec().displayName()),
            ResultsUtils.createThreadLabel(),
            ResultsUtils.createHostLabel(),
            ResultsUtils.createLanguageLabel("kotlin"),
            ResultsUtils.createFrameworkLabel("kotest"),
            ResultsUtils.createPackageLabel(pkgName),
            metadata.epic ?: ResultsUtils.createEpicLabel(pkgName),
            metadata.story,
            metadata.feature,
            metadata.severity,
            metadata.owner
        ).plus(metadata.customLabels)
    }
}