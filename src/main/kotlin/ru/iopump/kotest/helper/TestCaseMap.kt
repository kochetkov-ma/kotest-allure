package ru.iopump.kotest.helper

import io.kotest.core.test.Description
import io.kotest.core.test.TestCase
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class TestCaseMap {

    private val testCaseMap = ConcurrentHashMap<Description, AllureTestCase>()
    private val sourceFirstTestCaseMap = ConcurrentHashMap<String, Int>()
    private val rootTestCaseMap = ConcurrentHashMap<Description, MutableList<AllureTestCase>>()

    /**
     * Сохранить тест, сгенерировать uuid, задекорировать и вернуть [AllureTestCase]
     */
    fun put(testCase: TestCase): AllureTestCase {
        val uuid = uuid()
        return if (testCase.isTopLevel()) {
            testCase.addAsRootTc(uuid)
        } else {
            val rootAtcList = rootTestCaseMap[testCase.root()] ?: internalError(testCase.root())
            val rootAtc = rootAtcList.last()
            val isNewIteration = sourceFirstTestCaseMap[rootAtc.uuid] == testCase.line()

            if (isNewIteration) {
                val newIterAtc = rootAtc.copy(uuid = uuid())
                rootAtcList.add(newIterAtc)
                testCase.addAsNestedTc(uuid, newIterAtc, true)
            } else {
                testCase.addAsNestedTc(uuid, rootAtc)
            }
        }
    }

    /**
     * Вернуть тест верхнего уровня или исключение.
     */
    fun getRoot(testCase: TestCase): List<AllureTestCase> = rootTestCaseMap[testCase.desc()]
        ?: internalError(testCase.desc())

    /**
     * Вернуть вложенный тест.
     */
    fun getNested(testCase: TestCase): AllureTestCase = testCaseMap[testCase.desc()] ?: internalError(testCase.desc())

    //// PRIVATE ////
    private fun internalError(test: Any?): Nothing =
        throw IllegalArgumentException("Internal listener algorithm error. Call author. Debug information: '$test'")

    private fun TestCase.root() = desc().parents().first()
    private fun TestCase.desc() = this.description
    private fun TestCase.line() = this.source.lineNumber
    private fun uuid() = UUID.randomUUID().toString()
    private fun TestCase.addAsRootTc(uuid: String) = AllureTestCase(uuid, this, null, null).also { atc ->
        rootTestCaseMap.computeIfAbsent(this.desc()) { mutableListOf() }.add(atc)
        testCaseMap[this.desc()] = atc
    }

    private fun TestCase.addAsNestedTc(
        uuid: String,
        root: AllureTestCase,
        isNewIteration: Boolean = false
    ): AllureTestCase {
        val hasNestedParent = this.desc().parents().size >= 3
        val nestedParent = if (hasNestedParent) {
            testCaseMap[this.desc().parent]
        } else null
        return AllureTestCase(uuid, this, nestedParent, root, isNewIteration).also { atc ->
            testCaseMap[this.desc()] = atc
            sourceFirstTestCaseMap.putIfAbsent(root.uuid, this.line())
        }
    }
}

data class AllureTestCase(
    val uuid: String,
    val testCase: TestCase,
    val refToNestedParent: AllureTestCase?,
    val refToRoot: AllureTestCase?,
    val isNewIteration: Boolean = false
) {
    val isRoot by lazy { testCase.isTopLevel() }
    private val internalName: String = testCase.description.name.name

    private fun indexedName(index: Int): String {
        val suffix = if (index >= 1) " [$index]" else ""
        return "$internalName$suffix"
    }

    fun description(index: Int): Description = testCase.description.copy(
        name = testCase.description.name.copy(
            name = indexedName(index)
        )
    )
}