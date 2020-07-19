package ru.iopump.kotest.helper

import io.kotest.core.test.TestCase
import io.qameta.allure.*
import io.qameta.allure.model.Label
import io.qameta.allure.model.Link
import io.qameta.allure.util.AnnotationUtils.getLinks
import io.qameta.allure.util.ResultsUtils.*
import ru.iopump.kotest.annotation.JIRA_LINK
import ru.iopump.kotest.annotation.KDescription
import ru.iopump.kotest.annotation.KJira
import ru.iopump.kotest.annotation.KJiras
import java.util.regex.Pattern
import kotlin.reflect.full.findAnnotation

val JIRA_PATTER: Pattern = Pattern.compile("\\[([a-zA-Z]+-\\d+)]")

internal class AllureTestCaseProcessor(private val testCase: TestCase) {

    fun epic(): Label? = testCase.spec::class.findAnnotation<Epic>()?.let { createEpicLabel(it.value) }
    fun feature(): Label? = testCase.spec::class.findAnnotation<Feature>()?.let { createFeatureLabel(it.value) }
    fun severity(): Label? = testCase.spec::class.findAnnotation<Severity>()?.let { createSeverityLabel(it.value) }
    fun story(): Label? = testCase.spec::class.findAnnotation<Story>()?.let { createStoryLabel(it.value) }
    fun owner(): Label? = testCase.spec::class.findAnnotation<Owner>()?.let { createOwnerLabel(it.value) }
    fun allLinks(): Collection<Link> = (issues()
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
        .toSet()

    fun allDescriptions(): String? =
        listOfNotNull(kDescription()).joinToString(separator = System.lineSeparator())

    //// PRIVATE ////
    private fun kDescription(): String? = testCase.spec::class.findAnnotation<KDescription>()?.value

    private fun issues(): Collection<Link> =
        testCase.spec::class.findAnnotation<Issues>()?.value?.map { createLink(it) } ?: emptySet()

    private fun issue(): Link? = testCase.spec::class.findAnnotation<Issue>()?.let { createLink(it) }

    private fun links(): Collection<Link> =
        testCase.spec::class.findAnnotation<Links>()?.value?.map { createLink(it) } ?: emptySet()

    private fun link(): Link? = testCase.spec::class.findAnnotation<io.qameta.allure.Link>()?.let { createLink(it) }

    private fun tmsLinks(): Collection<Link> =
        testCase.spec::class.findAnnotation<TmsLinks>()?.value?.let { getLinks(it.toSet()) } ?: emptySet()

    private fun tmsLink(): Link? = testCase.spec::class.findAnnotation<TmsLink>()?.let { createLink(it) }

    private fun jiraLinks(): Collection<Link> =
        testCase.spec::class.findAnnotation<KJiras>()?.value?.let { getLinks(it.toSet()) } ?: emptySet()

    private fun jiraLink(): Collection<Link> =
        testCase.spec::class.findAnnotation<KJira>()?.let { getLinks(it) } ?: emptySet()

    private fun jiraLinksFromName(): Collection<Link> {
        val matcher = JIRA_PATTER.matcher(testCase.description.fullName())
        val links = mutableSetOf<Link>()
        while (matcher.find()) {
            val key = matcher.group(1)
            if (key.isNotBlank()) {
                links.add(createLink(key, key, null, JIRA_LINK))
            }
        }
        return links
    }
}