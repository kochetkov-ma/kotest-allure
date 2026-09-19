package ru.iopump.kotest.allure

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.kotest.core.spec.style.StringSpec
import java.nio.file.Files
import java.nio.file.Path

class AllureGeneratedResultContractSpec : StringSpec({
    "listener writes exact scenario statuses and nested Allure evidence" {
        // GIVEN fresh results from the isolated fixture execution.
        val results = jsonFiles(resultDirectory(), "-result.json")
        check(results.size == 7) { "Expected 7 fixture Allure results, found ${results.size}" }
        val byName = results.associateBy { it.string("name") }

        // WHEN scenario outcomes and listener metadata are read from emitted JSON.
        val expectedStatuses = fixtureStatuses

        // THEN statuses, hierarchy, attachment, labels, links, and descriptions match.
        check(byName.keys == expectedStatuses.keys) { "Fixture case names differ: ${byName.keys}" }
        check(byName.mapValues { it.value.string("status") } == expectedStatuses) {
            "Fixture Allure statuses differ: ${byName.mapValues { it.value.string("status") }}"
        }
        val passed = byName.getValue("fixture passed")
        check(passed.string("fullName").contains("fixture passed") && !passed.string("fullName").contains("FIX-10")) {
            "Fixture full name was not cleaned: ${passed.string("fullName")}"
        }
        check(passed.string("description") == "Listener contract fixture description") {
            "Fixture description was not retained"
        }
        val labels = passed.array("labels").map { it.asJsonObject.string("name") to it.asJsonObject.string("value") }.toMap()
        check(labels["epic"] == "Listener contract" && labels["tag"] == "contract-fixture" &&
            labels["AS_ID"] == "42" && labels["jira"] == "FIX-10") {
            "Fixture epic, tag, Allure ID, or issue label differs: $labels"
        }
        val links = passed.array("links").map { it.asJsonObject.string("url") }.toSet()
        check("https://example.org/listener-contract" in links) { "Fixture documentation link missing: $links" }
        check("https://example.org/jira/FIX-10" in links) { "Fixture issue link missing: $links" }
        check("https://example.org/tms/CASE-20" in links) { "Fixture TMS link missing: $links" }
        val outer = passed.array("steps").single().asJsonObject
        check(outer.string("name") == "fixture outer step" && outer.string("status") == "passed") {
            "Fixture outer step differs: $outer"
        }
        val inner = outer.array("steps").single().asJsonObject
        check(inner.string("name") == "fixture inner step" && inner.string("status") == "passed") {
            "Fixture inner step differs: $inner"
        }
        val attachment = inner.array("attachments").single().asJsonObject
        check(attachment.string("name") == "fixture attachment" && attachment.string("type") == "text/plain") {
            "Fixture attachment metadata differs: $attachment"
        }
        val attachmentContent = Files.readString(resultDirectory().resolve(attachment.string("source")))
        check(attachmentContent == "fixture attachment content") {
            "Fixture attachment content differs: $attachmentContent"
        }
        val caught = byName.getValue("fixture caught expected exception")
        val caughtStep = caught.array("steps").single().asJsonObject
        check(caughtStep.string("status") == "broken" && caughtStep.getAsJsonObject("statusDetails").string("message").contains("fixture expected exception")) {
            "Expected-exception annotation step status differs: $caughtStep"
        }
        val failed = byName.getValue("fixture failed assertion")
        check(failed.getAsJsonObject("statusDetails").string("message").contains("fixture deliberate assertion failure")) {
            "Assertion failure details were not retained"
        }
        val failingChild = failed.array("steps").single().asJsonObject
        check(failingChild.string("name") == "fixture failing child" && failingChild.string("status") == "failed") {
            "Failed child step did not propagate to scenario: $failingChild"
        }
        check(byName.getValue("fixture broken exception").getAsJsonObject("statusDetails").string("message").contains("fixture deliberate unexpected error")) {
            "Unexpected exception details were not retained"
        }
        val containers = jsonFiles(resultDirectory(), "-container.json")
        val specContainer = containers.single { container ->
            container.array("befores").any { it.asJsonObject.string("name") == "contract spec setup" }
        }
        val specChildUuids = specContainer.array("children").map { it.asString }.toSet()
        check(specChildUuids == results.map { it.string("uuid") }.toSet()) {
            "Spec container children differ from fixture result UUIDs: $specChildUuids"
        }
        check(specContainer.array("afters").single().asJsonObject.string("name") == "contract spec teardown") {
            "Spec teardown fixture missing or duplicated"
        }
    }

    "generated Allure HTML report contains fixture cases and summary" {
        // GIVEN a report generated from the fixture result directory.
        val report = reportDirectory()
        val index = Files.readString(report.resolve("index.html"))
        val summary = parse(report.resolve("widgets/summary.json"))
        val cases = jsonFiles(report.resolve("data/test-cases"), ".json")

        // WHEN the report summary and per-case data are inspected.
        val statistic = summary.getAsJsonObject("statistic")
        val caseNames = cases.map { it.string("name") }.toSet()

        // THEN the real viewer and its generated data represent all fixture outcomes.
        check(index.contains("allure")) { "Generated index.html does not contain the Allure viewer" }
        check(statistic.get("total").asInt == 7) { "Report total differs: $statistic" }
        check(statistic.get("passed").asInt == 4) { "Report passed count differs: $statistic" }
        check(statistic.get("failed").asInt == 1) { "Report failed count differs: $statistic" }
        check(statistic.get("broken").asInt == 1) { "Report broken count differs: $statistic" }
        check(statistic.get("skipped").asInt == 1) { "Report skipped count differs: $statistic" }
        check(caseNames == fixtureStatuses.keys) {
            "Report case names differ: $caseNames"
        }
    }
})

private val fixtureStatuses = mapOf(
    "fixture passed" to "passed",
    "fixture caught expected exception" to "passed",
    "fixture data scenario" to "passed",
    "fixture data scenario [1]" to "passed",
    "fixture skipped scenario" to "skipped",
    "fixture failed assertion" to "failed",
    "fixture broken exception" to "broken"
)

private fun resultDirectory(): Path = Path.of(System.getProperty("contract.results.dir") ?: error("contract.results.dir is missing"))

private fun reportDirectory(): Path = Path.of(System.getProperty("contract.report.dir") ?: error("contract.report.dir is missing"))

private fun jsonFiles(directory: Path, suffix: String): List<JsonObject> = Files.list(directory).use { paths ->
    paths.filter { it.fileName.toString().endsWith(suffix) }.sorted().map(::parse).toList()
}

private fun parse(path: Path): JsonObject = Files.newBufferedReader(path).use { JsonParser.parseReader(it).asJsonObject }

private fun JsonObject.string(field: String): String = get(field).asString

private fun JsonObject.array(field: String) = getAsJsonArray(field)?.toList().orEmpty()
