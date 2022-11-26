package ru.iopump.kotest.allure.helper.meta

import io.kotest.core.descriptors.Descriptor
import io.kotest.core.spec.Spec
import io.qameta.allure.Issues
import io.qameta.allure.Links
import io.qameta.allure.TmsLinks
import io.qameta.allure.model.Link
import io.qameta.allure.util.AnnotationUtils.getLinks
import io.qameta.allure.util.ResultsUtils.createLink
import io.qameta.allure.util.ResultsUtils.createTmsLink
import ru.iopump.kotest.allure.annotation.KJiras
import ru.iopump.kotest.allure.api.KotestAllureConstant.JIRA
import ru.iopump.kotest.allure.api.KotestAllureConstant.TMS
import ru.iopump.kotest.allure.helper.meta.AllureMetadataSupport.findAll
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

internal object AllureMetadataSupportLinks {

    internal inline val KClass<out Spec>?.linkAnnotations: Collection<Link> get() = this?.let { getLinks(it.java) }.orEmpty()

    internal inline val KClass<out Spec>?.issues: Collection<Link> get() = this?.findAnnotation<Issues>()?.value?.map { createLink(it) } ?: emptySet()

    internal inline val KClass<out Spec>?.links: Collection<Link> get() = this?.findAnnotation<Links>()?.value?.map { createLink(it) } ?: emptySet()

    internal inline val KClass<out Spec>?.tmsLinks: Collection<Link> get() = this?.findAnnotation<TmsLinks>()?.value?.let { getLinks(it.toSet()) } ?: emptySet()

    internal inline val KClass<out Spec>?.jiraLinks: Collection<Link> get() = this?.findAnnotation<KJiras>()?.value?.let { getLinks(it.toSet()) } ?: emptySet()

    internal inline val Descriptor?.jiraLinksFromTestName: Collection<Link>
        get() = this?.run {
            id.value
                .findAll(JIRA.PATTERN)
                .map { key -> createLink(key, key, null, JIRA.LINK_TYPE) }
                .toList()
        }.orEmpty()

    internal inline val Descriptor?.tmsLinksFromTestName: Collection<Link>
        get() = this?.run {
            id.value
                .findAll(TMS.PATTERN)
                .map { key -> createTmsLink(key) }
                .toList()
        }.orEmpty()
}