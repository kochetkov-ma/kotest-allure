package ru.iopump.kotest.allure

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.kotest.core.spec.style.StringSpec
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class LegacyExamplesResultContractSpec : StringSpec({
    "legacy examples retain their intentional XML outcomes" {
        // GIVEN the isolated legacy examples XML directory.
        val suites = Files.list(legacyXmlDirectory()).use { paths ->
            paths.filter { it.fileName.toString().startsWith("TEST-") && it.fileName.toString().endsWith(".xml") }
                .sorted().map { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it.toFile()).documentElement }
                .toList()
        }

        // WHEN case counts and failure identities are collected.
        val counts = listOf("tests", "failures", "errors", "skipped").associateWith { field ->
            suites.sumOf { it.getAttribute(field).toInt() }
        }
        val failures = suites.flatMap { suite ->
            suite.children("testcase").mapNotNull { test ->
                test.children("failure").singleOrNull()?.let { failure ->
                    "${suite.getAttribute("name").substringAfterLast('.')}|${test.getAttribute("name")}|${failure.getAttribute("message").lineSequence().first()}"
                }
            }
        }.groupingBy { it }.eachCount()

        // THEN the deliberate red examples are complete and unchanged.
        check(suites.size == 11) { "Expected 11 legacy XML suites, found ${suites.size}" }
        check(counts == mapOf("tests" to 57, "failures" to 12, "errors" to 0, "skipped" to 7)) {
            "Legacy XML counts differ: $counts"
        }
        check(failures == expectedLegacyFailures.groupingBy { it }.eachCount()) {
            "Legacy failure identities differ: $failures"
        }
    }

    "all-bang-disabled spec exposes the known Allure result gap" {
        // GIVEN XML, raw results, and containers from the isolated legacy execution.
        val results = legacyJsonFiles("-result.json")
        val containers = legacyJsonFiles("-container.json")
        val suite = "ru.iopump.kotest.allure.skip.ExampleAllSkippedFreeSpec"
        val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(legacyXmlDirectory().resolve("TEST-$suite.xml").toFile()).documentElement

        // WHEN the disabled spec's XML, result, and container identities are selected.
        val skippedRoots = results.filter { result ->
            result.getAsJsonArray("labels").any { label ->
                label.asJsonObject.get("name").asString == "suite" && label.asJsonObject.get("value").asString == suite
            }
        }
        val container = containers.single { it.get("name").asString == "ruiopumpkotestallureskipExampleAllSkippedFreeSpec" }
        val children = container.getAsJsonArray("children").map { it.asString }.toSet()
        val xmlSkipped = xml.children("testcase").associate { it.getAttribute("name") to (it.children("skipped").size == 1) }

        // THEN both XML roots are skipped; current Kotest callback path emits no Allure roots.
        check(xmlSkipped == mapOf("Scenario: 1" to true, "Scenario: 2" to true)) {
            "All-bang-disabled XML root statuses differ: $xmlSkipped"
        }
        check(skippedRoots.size == 0) {
            "Known all-bang-disabled Allure result gap changed: ${skippedRoots.map { it.get("name").asString }}"
        }
        check(children.size == 0) {
            "Known all-bang-disabled Allure container gap changed: $children"
        }
    }
})

private val expectedLegacyFailures = listOf(
    "ExampleBddSpec|Then: [PRJ-160] Nested step has been printed - FirstIterArg|java.lang.AssertionError: Step error1",
    "ExampleBddSpec|And: Step 2 has been printed too [PRJ-1300] - FirstIterArg|io.kotest.engine.extensions.ExtensionException\$BeforeAnyException: org.opentest4j.TestAbortedException: Previous step was failed. This will be skipped due to 'kotest.allure.skip.on.fail' option is true",
    "ExampleBddSpec|Then: [PRJ-160] Nested step has been printed - SecondIterArg|java.lang.AssertionError: Step error1",
    "ExampleBddSpec|And: Step 2 has been printed too [PRJ-1300] - SecondIterArg|io.kotest.engine.extensions.ExtensionException\$BeforeAnyException: org.opentest4j.TestAbortedException: Previous step was failed. This will be skipped due to 'kotest.allure.skip.on.fail' option is true",
    "ExampleFreeSpec|Step 2 has been printed too - --2--|java.lang.AssertionError: Only on --2-- iteration",
    "ExampleFreeSpec|Step 2 has been printed too - --2--|java.lang.AssertionError: Only on --2-- iteration",
    "ExampleStringSpec|Start kotest specification Scenario 2 #666|org.opentest4j.AssertionFailedError: Test failed for (it, \"--1--\") with error Step error1",
    "ExampleDisabledJunitFreeSpec|Step: should be disabled by junit|java.lang.IllegalStateException: should be disabled by junit but not",
    "ExampleFailFastFreeSpec|Step: failed 2|java.lang.IllegalStateException: Fail Fast ERROR",
    "ExampleFailInitFreeSpec|SpecInstantiationException|java.lang.reflect.InvocationTargetException",
    "ExampleFailInitFreeSpec|executionError|io.kotest.engine.spec.SpecInstantiationException: Could not create instance of class ru.iopump.kotest.allure.fast.ExampleFailInitFreeSpec",
    "ExampleSomeSkippedFreeSpec|Step 2 has been printed too - --2--|java.lang.AssertionError: Only on --2-- iteration"
)

private fun legacyXmlDirectory(): Path = Path.of(System.getProperty("contract.legacy.xml.dir") ?: error("contract.legacy.xml.dir is missing"))

private fun legacyResultsDirectory(): Path = Path.of(System.getProperty("contract.legacy.results.dir") ?: error("contract.legacy.results.dir is missing"))

private fun legacyJsonFiles(suffix: String): List<JsonObject> = Files.list(legacyResultsDirectory()).use { paths ->
    paths.filter { it.fileName.toString().endsWith(suffix) }.sorted()
        .map { path -> Files.newBufferedReader(path).use { JsonParser.parseReader(it).asJsonObject } }.toList()
}

private fun Element.children(tag: String): List<Element> = getElementsByTagName(tag).let { nodes ->
    (0 until nodes.length).map { nodes.item(it) as Element }
}
