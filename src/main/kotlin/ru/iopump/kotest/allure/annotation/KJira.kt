package ru.iopump.kotest.allure.annotation

import io.qameta.allure.LabelAnnotation
import io.qameta.allure.LinkAnnotation
import ru.iopump.kotest.allure.api.KotestAllureConstant.JIRA.LABEL_NAME
import ru.iopump.kotest.allure.api.KotestAllureConstant.JIRA.LINK_TYPE
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@LinkAnnotation(type = LINK_TYPE)
@LabelAnnotation(name = LABEL_NAME)
@Repeatable
annotation class KJira(val value: String)