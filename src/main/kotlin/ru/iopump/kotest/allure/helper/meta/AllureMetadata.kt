package ru.iopump.kotest.allure.helper.meta

import io.kotest.core.descriptors.Descriptor
import io.kotest.core.spec.Spec
import io.qameta.allure.model.Label
import io.qameta.allure.model.Link
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportDescriptions.kDescription
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLabels.allureIdsFromTestName
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLabels.epicFromPkg
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLabels.labelAnnotations
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLabels.severity
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLinks.issues
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLinks.jiraLinks
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLinks.jiraLinksFromTestName
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLinks.linkAnnotations
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLinks.links
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLinks.tmsLinks
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupportLinks.tmsLinksFromTestName
import kotlin.reflect.KClass

internal class AllureMetadata(
    specClass: KClass<out Spec>? = null,
    description: Descriptor? = null
) {

    internal val allLabels: List<Label> = buildList {
        addAll(specClass.labelAnnotations)
        add(specClass.severity)
        add(specClass.epicFromPkg)
        addAll(description.allureIdsFromTestName)
    }.filterNotNull()

    internal val allLinks: List<Link> = buildList {
        addAll(specClass.linkAnnotations)
        addAll(specClass.issues)
        addAll(specClass.links)
        addAll(specClass.tmsLinks)
        addAll(specClass.jiraLinks)
        addAll(description.jiraLinksFromTestName)
        addAll(description.tmsLinksFromTestName)
    }.asSequence()
        .filterNot { it.url.isNullOrBlank() }
        .distinctBy { it.url }
        .onEach { if (it.name.isBlank()) it.name = it.url }
        .distinct()
        .toList()

    internal val allDescriptions: String = buildList {
        add(specClass.kDescription)
    }.joinToString(separator = System.lineSeparator())
}