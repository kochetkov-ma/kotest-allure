package ru.iopump.kotest.allure.annotation

import io.kotest.core.spec.Spec

/**
 * Class annotation. Add description to all test cases in annotated [Spec] class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KDescription(val value: String)