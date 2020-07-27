Extended Allure Listener for Kotest
=================================

[![jdk11](https://camo.githubusercontent.com/f3886a668d85acf93f6fec0beadcbb40a5446014/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6a646b2d31312d7265642e737667)](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
[![kotlin](https://img.shields.io/badge/kotlin-1.3.2-green)](https://github.com/JetBrains/kotlin)
[![gradle](https://camo.githubusercontent.com/f7b6b0146f2ee4c36d3da9fa18d709301d91f811/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f746f6f6c2d677261646c652d626c75652e737667)](https://gradle.org/)
![GitHub](https://img.shields.io/github/license/kotest/kotest)
[![maven central](https://img.shields.io/maven-central/v/ru.iopump.kotest/kotest-allure)](http://search.maven.org/#search|ga|1|kotest-allure)

# Quick Start

### Add dependencies
```groovy
dependencies {
    testImplementation "ru.iopump.kotest:kotest-allure:$kotestAllureVersion"

    // Kotest deps https://github.com/kotest/kotest/blob/master/doc/reference.md#getting-started
    testImplementation 'io.kotest:kotest-runner-junit5-jvm:<version>' // For Kotest framework with transitives 'core' and 'common'
    // OR 
    // Instead of 'kotest-runner-junit5-jvm' you may use direct Kotest dependencies
    // testImplementation "io.kotest:kotest-core-jvm:<version>"
    // testImplementation "io.kotest:kotest-common-jvm:<version>"
}
```
Listener has annotation `@AutoScan`. It provides necessary `allure common libs` but doesn't offer Kotest dependency.

### Results
By default results were collected to `./build/allure-results`.
You can override it by system property `-Dallure.results.directory=...`. 
Or via gradle script:
```groovy
test {
    systemProperty "allure.results.directory", file("$buildDir/allure-results")
    useJUnitPlatform()
}
```
### API
#### Annotations
##### `KDescription` 
Specify test case description through this annotation with markdown text.
```kotlin
@KDescription("""
    This is multiline description.
    It must be a new line
""")
class ExampleBddSpec : BehaviorSpec() 
```

##### `KJira` 
Set link to Jira issue (not defect). You must adjust jira link pattern before by `allure.link.jira.pattern` variable.  
The best way is `allure.properties` in resource dir (or classpath)  
allure.properties:
```properties
allure.link.issue.pattern=https://example.org/issue/{}
allure.link.tms.pattern=https://example.org/tms/{}
allure.link.jira.pattern=https://example.org/jira/{}
```  
Code:
```kotlin
@KDescription("""
    This is multiline description.
    It must be a new line
""")
class ExampleBddSpec : BehaviorSpec() 
```

##### `KJiras`
Repeatable (annotation container) version of `KJira` annotation

#### Links in test name
You may specify jira link in a test name and even nested test name. Link must be matches with pattern `\\[([a-zA-Z]+-\\d+)]`.  
Use system variable `allure.jira.pattern` to override it.
```kotlin
class ExampleBddSpec : BehaviorSpec({
        Given("[PRJ-100] Start kotest specification Scenario") { println("...") }
})
```
`[PRJ-100]` consider as Jira link with key `PRJ-100` don't forget to define `allure.link.jira.pattern` in `allure.properties`

### All api features
See main feature above
WIP ...

### All features in generated reports

### Settings
There is a full settings table. All settings adjust by system variable:

| WIP | ... | 
|-----|-----|
|     |     |

# Public report example
See example generated report on [allure.iopump.ru](http://allure.iopump.ru/reports/kotest-allure)