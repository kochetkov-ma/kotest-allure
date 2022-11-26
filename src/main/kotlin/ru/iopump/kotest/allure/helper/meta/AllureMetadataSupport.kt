package ru.iopump.kotest.allure.helper.meta

object AllureMetadataSupport {

    internal fun String.findAll(patternWithOneGroup: Regex): Collection<String> =
        patternWithOneGroup
            .findAll(this)
            .mapNotNull { result -> if (result.groups.size >= 2) result.groups[1]?.value?.takeIf { it.isNotBlank() } else null }
            .toList()
}