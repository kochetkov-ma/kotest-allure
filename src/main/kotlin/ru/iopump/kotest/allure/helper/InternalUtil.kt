package ru.iopump.kotest.allure.helper

import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestResult.*
import io.qameta.allure.model.Label
import io.qameta.allure.model.Status
import io.qameta.allure.model.Status.*
import io.qameta.allure.model.StatusDetails
import io.qameta.allure.util.ResultsUtils
import org.opentest4j.TestAbortedException
import org.slf4j.LoggerFactory
import ru.iopump.kotest.allure.api.KotestAllureConstant
import ru.iopump.kotest.allure.api.KotestAllureConstant.VAR.SKIP_ON_FAIL
import ru.iopump.kotest.allure.api.KotestAllureConstant.VAR.STEP_SHOULD_THROW_SUPPORT
import ru.iopump.kotest.allure.api.KotestAllureConstant.VAR.TEST_NAME_AUTO_CLEAN_UP
import ru.iopump.kotest.allure.api.KotestAllureExecution.bestName
import ru.iopump.kotest.allure.helper.meta.AllureMetadata
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.System.getProperty
import java.lang.System.getenv
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.full.isSubclassOf

internal object InternalUtil {

    private val isAllureMetaCleanUp = TEST_NAME_AUTO_CLEAN_UP.prop(true)

    private val isStepShouldThrowSupportEnabled = STEP_SHOULD_THROW_SUPPORT.prop(true)

    internal inline fun <reified T> T.toOptional() = Optional.ofNullable(this)

    internal fun String.logger() = LoggerFactory.getLogger(this)

    internal inline fun <reified T> logger() = LoggerFactory.getLogger(T::class.java)

    internal inline fun <reified T> String.prop(default: T): T = prop() ?: default

    internal inline fun <reified T> String.prop(): T? = (
        getProperty(this)
            ?: getProperty(uppercase(Locale.getDefault()))
            ?: getProperty(lowercase(Locale.getDefault()))
            ?: getenv(this)
            ?: getenv(uppercase(Locale.getDefault()))
            ?: getenv(lowercase(Locale.getDefault()))
            ?: getenv(replace(".", "_"))
            ?: getenv(replace(".", "_").uppercase(Locale.getDefault()))
            ?: getenv(replace(".", "_").lowercase(Locale.getDefault()))
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

    internal val String?.safeFileName
        get() = this?.replace("[^\\sа-яА-Яa-zA-Z0-9]".toRegex(), "")
            ?.replace("\\s{2,}".toRegex(), "_")
            ?.let { it.takeIf { it.length <= 130 } ?: (it.take(120) + it.hashCode()) } ?: "null"

    internal fun TestResult.toAllure(): Pair<Status, StatusDetails> {
        val status = when (this) {
            is Error -> BROKEN
            is Failure -> FAILED
            is Ignored -> SKIPPED
            is Success -> PASSED
        }

        val details = this.errorOrNull?.let { throwable ->
            StatusDetails().apply {
                this.message = throwable.toString()
                this.trace = throwable.readStackTrace()
            }
        } ?: StatusDetails()

        return status to details
    }

    internal fun AllureTestResult.updateTestResult(testUuid: String, test: TestCase, meta: AllureMetadata, i: Int = 0) {
        val suffix = " [$i]".takeIf { i >= 1 }.orEmpty()
        val index = "$i".takeIf { i >= 1 }.orEmpty()
        uuid = testUuid

        name = test.name.testName.allureMetaCleanUp() + suffix
        description = meta.allDescriptions

        fullName = test.descriptor.bestName().allureMetaCleanUp() + index
        testCaseId = test.descriptor.bestName().allureMetaCleanUp() + index
        historyId = test.descriptor.bestName().allureMetaCleanUp() + index
        labels = testCaseLabels(test, meta)
        links = meta.allLinks
    }

    internal fun AllureStepResult.updateStepResult(testCase: TestCase, metadata: AllureMetadata) {
        name = testCase.name.testName
        description = metadata.allDescriptions
    }

    internal fun AllureTestResult.updateStatus(statusAndDetails: Pair<Status, StatusDetails>) {
        // Kotest 5.4.X listener doesn't take into account child steps results.
        // We should find previous fail / broken child steps in ALLURE storage and use it on top
        val closestPreviousErrorOrBrokenStatusAndDetails: Pair<Status, StatusDetails> = statusAndDetails.takeIf { isStepShouldThrowSupportEnabled }.let {
            steps.map { it.status to it.statusDetails }.lastOrNull { it.first.isBrokenOrFailed } ?: statusAndDetails
        }

        val effectiveStatusAndDetails =
            if (statusAndDetails.first.isNotPassed) statusAndDetails else closestPreviousErrorOrBrokenStatusAndDetails

        val currentStatus = this.status
        val needUpdate = currentStatus == null  // если еще нет статуса
            || currentStatus == PASSED  // если текущий статус пройден
            || currentStatus == SKIPPED // если статус пропущен
        if (needUpdate) {
            this.status = effectiveStatusAndDetails.first
            this.statusDetails = effectiveStatusAndDetails.second
        }
    }

    internal fun AllureStepResult.updateStatus(statusAndDetails: Pair<Status, StatusDetails>) {
        val currentStatus = this.status
        val needUpdate = currentStatus == null // если еще нет статуса
            || currentStatus == PASSED // если текущий статус пройден
            || currentStatus == SKIPPED // если статус пропущен
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

    private fun String.allureMetaCleanUp() =
        if (isAllureMetaCleanUp)
            replace(KotestAllureConstant.JIRA.PATTERN, "")
                .replace(KotestAllureConstant.TMS.PATTERN, "")
                .replace(KotestAllureConstant.ALLURE_ID.PATTERN, "")
                .trim()
        else this

    private val brokenOrFailed: Array<Status> = arrayOf(BROKEN, FAILED)

    private val skipOnFail: Boolean = SKIP_ON_FAIL.prop(true)

    private val AllureTestResult.isBad: Boolean get() = status?.isBrokenOrFailed ?: false

    private val Status?.isBrokenOrFailed: Boolean get() = this in brokenOrFailed

    private val Status?.isNotPassed get() = this != PASSED

    private fun Throwable.readStackTrace(): String {
        val stringWriter = StringWriter()
        PrintWriter(stringWriter).use { printWriter -> printStackTrace(printWriter) }
        return stringWriter.toString()
    }

    private fun testCaseLabels(testCase: TestCase, metadata: AllureMetadata): List<Label> {
        val pkgName = testCase.spec::class.java.`package`.name

        return listOfNotNull(
            ResultsUtils.createSuiteLabel(testCase.descriptor.spec().id.value),
            ResultsUtils.createThreadLabel(),
            ResultsUtils.createHostLabel(),
            ResultsUtils.createLanguageLabel("kotlin"),
            ResultsUtils.createFrameworkLabel("kotest"),
            ResultsUtils.createPackageLabel(pkgName)
        ).plus(metadata.allLabels)
    }
}