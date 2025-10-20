package ru.iopump.kotest.allure.helper

import io.kotest.core.test.TestCase
import io.qameta.allure.model.StepResult
import io.qameta.allure.model.TestResult

typealias AllureTestResult = TestResult
typealias AllureStepResult = StepResult
typealias KotestTestResult = io.kotest.engine.test.TestResult
typealias KotestTestCase = TestCase