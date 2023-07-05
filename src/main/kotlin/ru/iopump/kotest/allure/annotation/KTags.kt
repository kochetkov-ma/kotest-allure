package ru.iopump.kotest.allure.annotation

import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS
)
annotation class KTags(val value: Array<KTag>)