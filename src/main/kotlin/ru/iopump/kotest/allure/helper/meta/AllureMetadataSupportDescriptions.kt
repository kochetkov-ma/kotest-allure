package ru.iopump.kotest.allure.helper.meta

import io.kotest.core.spec.Spec
import ru.iopump.kotest.allure.annotation.KDescription
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

internal object AllureMetadataSupportDescriptions {

    internal inline val KClass<out Spec>?.kDescription: String get() = this?.findAnnotation<KDescription>()?.value.orEmpty()
}