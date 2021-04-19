package ru.iopump.kotest.allure.api

import io.qameta.allure.AllureLifecycle
import io.qameta.allure.model.StepResult
import org.slf4j.Logger
import ru.iopump.kotest.allure.api.KotestAllureConstant.VAR
import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8

/**
 * Decorate [startStep] and [addAttachment] with SLF4J [Logger]. Using by default.
 * You may set custom lifecycle via env/sys var [VAR.ALLURE_LIFECYCLE_CLASS].
 */
open class Slf4JAllureLifecycle(private val logger: Logger) : AllureLifecycle() {

    override fun startStep(parentUuid: String?, uuid: String?, result: StepResult?) =
        super.startStep(parentUuid, uuid, result).also { logger.info("STEP: ${result.log}") }

    override fun addAttachment(name: String?, type: String?, fileExtension: String?, stream: InputStream?) {
        when (logger.isDebugEnabled) {
            true -> stream.use { io ->
                runCatching { io?.readBytes() ?: "".toByteArray(UTF_8) }
                    .getOrElse { it.localizedMessage.toByteArray(UTF_8) }
            }.apply {
                super.addAttachment(name, type, fileExtension, this.inputStream()).also {
                    logger.debug("ATTACHMENT: $name $type $fileExtension\n{}", this.toString(UTF_8).take(2000))
                }
            }
            false -> super.addAttachment(name, type, fileExtension, stream)
        }
    }

    /////////////////
    //// PRIVATE ////
    /////////////////

    private val StepResult?.log
        get() = when (this) {
            null -> ""
            else -> "${name ?: ""} ${status ?: ""}" + (
                    params.entries
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString("\n", "\n") { "${it.key}: ${it.value}" } ?: ""
                    )
        }

    private val StepResult?.params
        get() = mapOf(
            "description" to this?.description,
            "parameters" to this?.parameters?.joinToString(prefix = "[", postfix = "]") { "${it.name}=${it.value}" },
            "attachments" to this?.attachments?.joinToString(
                prefix = "[",
                postfix = "]"
            ) { "${it.name}=${it.type}(${it.source.length})" }
        ).filterNot { it.value.isNullOrBlank() || it.value == "[]" }
}