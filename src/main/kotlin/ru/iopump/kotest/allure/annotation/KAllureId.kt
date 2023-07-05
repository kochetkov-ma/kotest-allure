package ru.iopump.kotest.allure.annotation

import io.qameta.allure.LabelAnnotation
import io.qameta.allure.util.ResultsUtils
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@LabelAnnotation(name = ResultsUtils.ALLURE_ID_LABEL_NAME)
annotation class KAllureId(val value: String)
