package ru.iopump.kotest

import io.qameta.allure.Step

@Step("Allure aspectj step annotation on method 'step1'")
internal fun step1() {
    println("Execute step-1")
}

@Step("Allure aspectj step annotation on method 'step2'")
internal fun step2() {
    println("Execute step-2")
    stepNested()
}

@Step("Allure aspectj nested step annotation on method 'stepNested'")
internal fun stepNested() {
    println("Execute nested step")
}

@Step("Allure aspectj step throw exception")
internal fun stepException() {
    throw AssertionError("Step error")
}