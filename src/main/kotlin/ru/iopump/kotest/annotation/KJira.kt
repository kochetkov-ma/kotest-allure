package ru.iopump.kotest.annotation

import io.qameta.allure.LinkAnnotation
import java.lang.annotation.Inherited

const val JIRA_LINK = "jira"

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@LinkAnnotation(type = JIRA_LINK)
@Repeatable
annotation class KJira(val value: String)