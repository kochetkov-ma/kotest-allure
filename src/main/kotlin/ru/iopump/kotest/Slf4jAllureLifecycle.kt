package ru.iopump.kotest

import io.qameta.allure.AllureLifecycle
import io.qameta.allure.model.StepResult
import org.slf4j.LoggerFactory
import java.io.InputStream

class Slf4jAllureLifecycle : AllureLifecycle() {
    companion object {
        private val log = LoggerFactory.getLogger(Slf4jAllureLifecycle::class.java)
    }

    override fun startStep(parentUuid: String?, uuid: String?, result: StepResult?) {
        log.info("[ALLURE STEP] " + toLog(result))
        super.startStep(parentUuid, uuid, result)
    }

    override fun addAttachment(name: String?, type: String?, fileExtension: String?, stream: InputStream?) {
        val content = if (stream == null) {
            "empty"
        } else {
            stream.bufferedReader().runCatching { readText() }.getOrElse { it.localizedMessage }
        }

        log.debug("[ALLURE ATTACHMENT] $name $type [$fileExtension]\n$content")
        super.addAttachment(name, type, fileExtension, stream)
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