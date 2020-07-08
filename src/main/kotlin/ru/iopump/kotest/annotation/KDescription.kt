package ru.iopump.kotest.annotation

/**
 * Class annotation. Add description to all test cases in annotated class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KDescription(val value: String)