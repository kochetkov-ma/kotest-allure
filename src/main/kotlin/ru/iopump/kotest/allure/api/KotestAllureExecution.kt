package ru.iopump.kotest.allure.api

import io.kotest.core.spec.Spec
import io.kotest.core.test.Description
import io.kotest.core.test.TestCase
import io.qameta.allure.Allure
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.model.FixtureResult
import io.qameta.allure.model.Status
import ru.iopump.kotest.allure.KotestAllureListener
import ru.iopump.kotest.allure.api.KotestAllureConstant.VAR
import ru.iopump.kotest.allure.api.KotestAllureExecution.PROJECT_UUID
import ru.iopump.kotest.allure.api.KotestAllureExecution.containerUuid
import ru.iopump.kotest.allure.api.KotestAllureExecution.setUpFixture
import ru.iopump.kotest.allure.helper.InternalUtil.logger
import ru.iopump.kotest.allure.helper.InternalUtil.prop
import ru.iopump.kotest.allure.helper.InternalUtil.safeFileName
import java.io.File
import java.util.UUID
import kotlin.reflect.KClass

/**
 * User API to get access to allure listener execution.
 * For example: create fixture [setUpFixture] or obtain actual [Spec] uuid in Allure Storage - [containerUuid]
 * or project execution root uuid - [PROJECT_UUID]
 */
object KotestAllureExecution {
    private val log = logger<KotestAllureExecution>()

    /**
     * Get current [AllureLifecycle] or extended version for example [Slf4JAllureLifecycle]
     */
    val ALLURE: AllureLifecycle = initAllureLifecycle()

    /**
     * Get container uuid of entire Execution.
     *
     * See [setUpFixture]
     * See [tearDownFixture]
     * See [EXECUTION_START_CALLBACK]
     */
    val PROJECT_UUID = KotestAllureListener.hashCode().toString()

    /**
     * Use to add project level Fixture.
     * Not thread safe. Not final variable.
     *
     * See [setUpFixture]
     * See [tearDownFixture]
     */
    var EXECUTION_START_CALLBACK: (projectUuid: String) -> Unit = { }

    /**
     * Get container uuid of [Spec]
     *
     * See [setUpFixture]
     * See [tearDownFixture]
     */
    val Spec.containerUuid get() = this::class.containerUuid

    /**
     * Get container uuid of [Spec] class
     *
     * See [setUpFixture]
     * See [tearDownFixture]
     */
    val KClass<out Spec>.containerUuid get() = qualifiedName.safeFileName

    /**
     * The longest name for [TestCase.description]
     */
    fun Description.bestName() = names().joinToString("_") { it.displayName }

    /**
     * Create Set Up Fixture for [Spec]
     */
    fun Spec.setUpFixture(
        name: String,
        atomic: Boolean = true,
        fixtureResult: FixtureResult.() -> Unit = {}
    ) = this::class.setUpFixture(name, atomic, fixtureResult)

    /**
     * Create Set Up Fixture for [Spec]
     */
    fun KClass<out Spec>.setUpFixture(
        name: String,
        atomic: Boolean = true,
        fixtureResult: FixtureResult.() -> Unit = {}
    ): String = containerUuid.setUpFixture(name, atomic, fixtureResult)

    /**
     * Create Set Up Fixture by container uuid.
     *
     * See [PROJECT_UUID]
     * See [EXECUTION_START_CALLBACK]
     * See [containerUuid]
     */
    fun String.setUpFixture(
        name: String,
        atomic: Boolean = true,
        fixtureResult: FixtureResult.() -> Unit = {}
    ): String {
        val fixture = FixtureResult()
            .also { it.name = name; it.status = Status.PASSED }
            .also(fixtureResult)

        val uuid = UUID.randomUUID().toString()
        ALLURE.startPrepareFixture(this, uuid, fixture)
        if (atomic) ALLURE.stopFixture(uuid)
        return uuid
    }

    /**
     * Create Tear Down Fixture for [Spec]
     */
    fun Spec.tearDownFixture(
        name: String,
        atomic: Boolean = true,
        fixtureResult: FixtureResult.() -> Unit = {}
    ) = this::class.tearDownFixture(name, atomic, fixtureResult)

    /**
     * Create Tear Down Fixture for [Spec]
     */
    fun KClass<out Spec>.tearDownFixture(
        name: String,
        atomic: Boolean = true,
        fixtureResult: FixtureResult.() -> Unit = {}
    ): String = containerUuid.tearDownFixture(name, atomic, fixtureResult)

    /**
     * Create Tear Down Fixture by container uuid.
     *
     * See [PROJECT_UUID]
     * See [EXECUTION_START_CALLBACK]
     * See [containerUuid]
     */
    fun String.tearDownFixture(
        name: String,
        atomic: Boolean = true,
        fixtureResult: FixtureResult.() -> Unit = {}
    ): String {
        val fixture = FixtureResult()
            .also { it.name = name; it.status = Status.PASSED }
            .also(fixtureResult)

        val uuid = UUID.randomUUID().toString()
        ALLURE.startTearDownFixture(this, uuid, fixture)
        if (atomic) ALLURE.stopFixture(uuid)
        return uuid
    }

    /////////////////
    //// PRIVATE ////
    /////////////////

    private fun initAllureLifecycle(): AllureLifecycle {
        val resultDir = VAR.ALLURE_RESULTS_DIR.prop("build/allure-results")
        val slf4jEnabled = VAR.ALLURE_SLF4J_LOG.prop(true)
        val allureClassRef: String = VAR.ALLURE_LIFECYCLE_CLASS.prop("")

        System.setProperty(VAR.ALLURE_RESULTS_DIR, resultDir)
        clearPreviousResults(File(resultDir))

        return (if (allureClassRef.isNotBlank())
            runCatching { Class.forName(allureClassRef).getConstructor().newInstance() as AllureLifecycle }
                .onFailure { throw RuntimeException("Cannot create AllureLifecycle from class '$allureClassRef'", it) }
                .getOrThrow()
        else (if (slf4jEnabled) Slf4JAllureLifecycle(log) else AllureLifecycle())).also { Allure.setLifecycle(it) }
    }

    private fun clearPreviousResults(dir: File) {
        if (VAR.CLEAR_ALLURE_RESULTS_DIR.prop(true)) {
            if (dir.exists() && dir.isDirectory) {
                runCatching { dir.deleteRecursively() }.getOrElse { log.error("Cannot delete '$dir'", it) }
            }
        }
    }
}