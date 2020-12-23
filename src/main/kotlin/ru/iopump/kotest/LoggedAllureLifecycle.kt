package ru.iopump.kotest

import io.qameta.allure.AllureLifecycle
import io.qameta.allure.model.StepResult
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8

class LoggedAllureLifecycle : AllureLifecycle() {
    private companion object {
        private val log = LoggerFactory.getLogger(LoggedAllureLifecycle::class.java)
    }

    override fun startStep(parentUuid: String?, uuid: String?, result: StepResult?) =
        super.startStep(parentUuid, uuid, result).also { log.info("[ALLURE-STEP] ${result.log}") }

    override fun addAttachment(name: String?, type: String?, fileExtension: String?, stream: InputStream?) {
        when (log.isDebugEnabled) {
            true -> stream.use { io ->
                runCatching { io?.readAllBytes() ?: "".toByteArray(UTF_8) }.getOrElse { it.localizedMessage.toByteArray(UTF_8) }
            }.apply {
                super.addAttachment(name, type, fileExtension, this.inputStream()).also {
                    log.debug("[ALLURE-ATTACHMENT] $name $type $fileExtension\n{}", toString(UTF_8).take(2000))
                }
            }
            false -> super.addAttachment(name, type, fileExtension, stream)
        }
    }

    //// PRIVATE ////
    private val StepResult?.log
        get() = when (this) {
            null -> ""
            else -> "${name ?: ""} ${status ?: ""}" + params.entries
                .takeIf { it.isNotEmpty() }
                ?.joinToString("\n", "\n") { "${it.key}: ${it.value}" }
        }

    private val StepResult?.params
        get() = mapOf(
            "description" to this?.description,
            "parameters" to this?.parameters?.joinToString(prefix = "[", postfix = "]") { "${it.name}=${it.value}" },
            "attachments" to this?.attachments?.joinToString(prefix = "[", postfix = "]") { "${it.name}=${it.type}(${it.source.length})" }
        ).filterNot { it.value.isNullOrBlank() || it.value == "[]" }
}