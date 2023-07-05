package ru.iopump.kotest.allure.annotation

import io.qameta.allure.LabelAnnotation
import io.qameta.allure.util.ResultsUtils.TAG_LABEL_NAME
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@LabelAnnotation(name = TAG_LABEL_NAME)
@Repeatable
annotation class KTag(val value: String)
