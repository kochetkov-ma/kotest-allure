package ru.iopump.kotest.helper

import io.kotest.core.test.Description
import io.kotest.core.test.TestCase
import java.util.*

internal class TestCaseMap {

    private val testCaseMap = mutableMapOf<Description, AllureTestCase>()
    private val sourceFirstTestCaseMap = mutableMapOf<String, Int>()
    private val rootTestCaseMap = mutableMapOf<Description, MutableList<AllureTestCase>>()

    fun put(testCase: TestCase): AllureTestCase {
        val uuid = uuid()
        return if (testCase.isTopLevel()) {
            testCase.addAsRootTc(uuid)
        } else {
            val rootAtcList = rootTestCaseMap[testCase.root()]!!
            val rootAtc = rootAtcList.last()
            val isNewIteration = sourceFirstTestCaseMap[rootAtc.uuid] == testCase.line()

            if (isNewIteration) {
                val newIterAtc = rootAtc.copyAsNextIter(uuid())
                rootAtcList.add(newIterAtc)
                testCase.addAsNestedTc(uuid, newIterAtc, true)
            } else {
                testCase.addAsNestedTc(uuid, rootAtc)
            }
        }
    }

    fun getRoot(testCase: TestCase): List<AllureTestCase> = rootTestCaseMap[testCase.desc()]!!

    fun getNested(testCase: TestCase): AllureTestCase = testCaseMap[testCase.desc()]!!

    private fun TestCase.root() = Description(this.description.parents.dropLast(1), this.description.parents[1])
    private fun TestCase.desc() = this.description
    private fun TestCase.line() = this.source.lineNumber
    private fun uuid() = UUID.randomUUID().toString()

    private fun TestCase.addAsRootTc(uuid: String) = AllureTestCase(uuid, this, true, null).also { atc ->
        rootTestCaseMap.computeIfAbsent(this.desc()) { mutableListOf() }.add(atc)
        testCaseMap[this.desc()] = atc
    }

    private fun TestCase.addAsNestedTc(
        uuid: String,
        root: AllureTestCase,
        isNewIteration: Boolean = false
    ): AllureTestCase =
        AllureTestCase(uuid, this, false, root, isNewIteration).also { atc ->
            testCaseMap[this.desc()] = atc
            sourceFirstTestCaseMap.putIfAbsent(root.uuid, this.line())
        }
}

data class AllureTestCase(
    val uuid: String,
    val testCase: TestCase,
    val isRoot: Boolean,
    val refToRoot: AllureTestCase?,
    val isNewIteration: Boolean = false
) {
    private val name: String = testCase.description.name.name

    fun name(index: Int): String {
        val suffix = if (index >= 1) " [$index]" else ""
        return "$name$suffix"
    }

    fun description(index: Int): Description =
        testCase.description.copy(
            name = testCase.description.name.copy(
                name = name(index)
            )
        )

    fun copyAsNextIter(uuid: String): AllureTestCase = copy(uuid = uuid)
}