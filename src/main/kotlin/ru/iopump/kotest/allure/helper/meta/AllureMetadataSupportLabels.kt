package ru.iopump.kotest.allure.helper.meta

import io.kotest.core.descriptors.Descriptor
import io.kotest.core.spec.Spec
import io.qameta.allure.Epic
import io.qameta.allure.Severity
import io.qameta.allure.model.Label
import io.qameta.allure.util.AnnotationUtils
import io.qameta.allure.util.ResultsUtils.*
import ru.iopump.kotest.allure.api.KotestAllureConstant
import ru.iopump.kotest.allure.api.KotestAllureConstant.JIRA
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupport.findAll
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

internal object AllureMetadataSupportLabels {

    internal inline val KClass<out Spec>?.labelAnnotations: Set<Label> get() = this?.let { AnnotationUtils.getLabels(it.java) }.orEmpty()

    internal inline val KClass<out Spec>?.epicFromPkg get() = this?.run { if (hasAnnotation<Epic>()) null else createEpicLabel(this.java.`package`.name) }

    internal inline val KClass<out Spec>?.severity: Label? get() = this?.findAnnotation<Severity>()?.let { createSeverityLabel(it.value) }

    internal inline val Descriptor?.allureIdsFromTestName: Collection<Label>
        get() = this?.run {
            id.value
                .findAll(KotestAllureConstant.ALLURE_ID.PATTERN)
                .map { key -> createLabel(ALLURE_ID_LABEL_NAME, key) }
                .toList()
        }.orEmpty()

    internal inline val Descriptor?.jiraLabelsFromTestName: Collection<Label>
        get() = this?.run {
            id.value
                .findAll(JIRA.PATTERN)
                .map { key -> createLabel(JIRA.LABEL_NAME, key) }
                .toList()
        }.orEmpty()
}