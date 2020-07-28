package ru.iopump.kotest

import io.qameta.allure.AllureLifecycle
import io.qameta.allure.model.StepResult
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8

class Slf4jAllureLifecycle : AllureLifecycle() {
    companion object {
        private val log = LoggerFactory.getLogger(Slf4jAllureLifecycle::class.java)
    }

    override fun startStep(parentUuid: String?, uuid: String?, result: StepResult?) {
        log.info("[ALLURE STEP] " + toLog(result))
        super.startStep(parentUuid, uuid, result)
    }

    override fun addAttachment(name: String?, type: String?, fileExtension: String?, stream: InputStream?) {
        if (!log.isDebugEnabled) {
            super.addAttachment(name, type, fileExtension, stream)
        } else {
            val byteArray = stream.use { io ->
                io.runCatching { this?.readAllBytes() ?: "empty".toByteArray(UTF_8) }
                        .getOrElse { it.localizedMessage.toByteArray(UTF_8) }
            }
            log.debug("[ALLURE ATTACHMENT] $name $type [$fileExtension]\n{}", byteArray.toString(UTF_8))
            super.addAttachment(name, type, fileExtension, byteArray.inputStream())
        }
    }

    private fun toLog(stepResult: StepResult?) = if (stepResult == null) {
        "empty"
    } else {
        """${stepResult.name} - ${stepResult.status ?: "PASSED"} 
description : ${stepResult.description}
parameters  : ${stepResult.parameters.joinToString(System.lineSeparator()) { it.name + " = " + it.value }}
attachments : ${stepResult.attachments.joinToString(System.lineSeparator()) { it.name + " = " + it.source }}"""
    }
}