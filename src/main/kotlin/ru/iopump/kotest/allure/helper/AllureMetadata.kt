package ru.iopump.kotest.allure.helper

import io.kotest.core.spec.Spec
import io.kotest.core.test.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Issue
import io.qameta.allure.Issues
import io.qameta.allure.Links
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import io.qameta.allure.TmsLinks
import io.qameta.allure.model.Label
import io.qameta.allure.model.Link
import io.qameta.allure.util.AnnotationUtils.getLabels
import io.qameta.allure.util.AnnotationUtils.getLinks
import io.qameta.allure.util.ResultsUtils.*
import ru.iopump.kotest.allure.annotation.*
import ru.iopump.kotest.allure.api.KotestAllureConstant
import ru.iopump.kotest.allure.api.KotestAllureConstant.JIRA
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

internal class AllureMetadata(
    private val specClass: KClass<out Spec>? = null,
    private val description: Description? = null
) {

    val epic: Label? = specClass?.findAnnotation<Epic>()?.let { createEpicLabel(it.value) }
    val feature: Label? = specClass?.findAnnotation<Feature>()?.let { createFeatureLabel(it.value) }
    val severity: Label? = specClass?.findAnnotation<Severity>()?.let { createSeverityLabel(it.value) }
    val story: Label? = specClass?.findAnnotation<Story>()?.let { createStoryLabel(it.value) }
    val owner: Label? = specClass?.findAnnotation<Owner>()?.let { createOwnerLabel(it.value) }
    val allLinks: List<Link> = (issues()
            + issue()
            + links()
            + link()
            + tmsLinks()
            + tmsLink()
            + jiraLinks()
            + jiraLink()
            + jiraLinksFromName()).asSequence().filterNotNull().filterNot { it.url.isNullOrBlank() }
        .distinctBy { it.url }
        .onEach { if (it.name.isBlank()) it.name = it.url }
        .distinct()
        .toList()

    val customLabels: List<Label> = (jiraLabel()
            + jiraLabels()
            + jiraLabelsFromName()
            + allureIdLabel()
            + allureIdsFromTestName()
            + tagLabel()
            + tagLabels()).asSequence()
        .filterNot { it.value.isNullOrBlank() }
        .distinctBy { it.name + it.value }
        .toList()

    val allDescriptions: String =
        listOfNotNull(kDescription()).joinToString(separator = System.lineSeparator())

    //// PRIVATE ////
    private fun kDescription(): String? = specClass?.findAnnotation<KDescription>()?.value

    private fun issues(): Collection<Link> =
        specClass?.findAnnotation<Issues>()?.value?.map { createLink(it) } ?: emptySet()

    private fun issue(): Link? = specClass?.findAnnotation<Issue>()?.let { createLink(it) }

    private fun links(): Collection<Link> =
        specClass?.findAnnotation<Links>()?.value?.map { createLink(it) } ?: emptySet()

    private fun link(): Link? = specClass?.findAnnotation<io.qameta.allure.Link>()?.let { createLink(it) }

    private fun tmsLinks(): Collection<Link> =
        specClass?.findAnnotation<TmsLinks>()?.value?.let { getLinks(it.toSet()) } ?: emptySet()

    private fun tmsLink(): Link? = specClass?.findAnnotation<TmsLink>()?.let { createLink(it) }

    private fun jiraLinks(): Collection<Link> =
        specClass?.findAnnotation<KJiras>()?.value?.let { getLinks(it.toSet()) } ?: emptySet()

    private fun jiraLink(): Collection<Link> =
        specClass?.findAnnotation<KJira>()?.let { getLinks(it) } ?: emptySet()

    private fun jiraLinksFromName(): Collection<Link> {
        if (description == null) return emptyList()
        return JIRA.PATTERN.findAll(description.path().value).mapNotNull { result ->
            if (result.groups.size >= 2)
                result.groups[1]?.value?.takeIf { it.isNotBlank() }?.let { key ->
                    createLink(key, key, null, JIRA.LINK_TYPE)
                }
            else null
        }.toList()
    }

    private fun jiraLabel(): Collection<Label> =
        specClass?.findAnnotation<KJira>()?.let { getLabels(it) } ?: emptySet()

    private fun jiraLabels(): Collection<Label> =
        specClass?.findAnnotation<KJiras>()?.value?.let { getLabels(it.toSet()) } ?: emptySet()

    private fun jiraLabelsFromName(): Collection<Label> {
        if (description == null) return emptyList()
        return JIRA.PATTERN.findAll(description.path().value).mapNotNull { result ->
            if (result.groups.size >= 2)
                result.groups[1]?.value?.takeIf { it.isNotBlank() }?.let { key ->
                    createLabel(JIRA.LABEL_NAME, key)
                }
            else null
        }.toList()
    }

    private fun allureIdsFromTestName(): Collection<Label> {
        if (description == null) return emptyList()
        return KotestAllureConstant.ALLURE_ID.PATTERN.findAll(description.path().value).mapNotNull { result ->
            if (result.groups.size >= 2)
                result.groups[1]?.value?.takeIf { it.isNotBlank() }?.let { key ->
                    createLabel(ALLURE_ID_LABEL_NAME, key)
                }
            else null
        }.toList()
    }

    private fun allureIdLabel(): Collection<Label> =
        specClass?.findAnnotation<KAllureId>()?.let { getLabels(it) } ?: emptySet()

    private fun tagLabel(): Collection<Label> =
        specClass?.findAnnotation<KTag>()?.let { getLabels(it) } ?: emptySet()

    private fun tagLabels(): Collection<Label> =
        specClass?.findAnnotation<KTags>()?.value?.let { getLabels(it.toSet()) } ?: emptySet()
}