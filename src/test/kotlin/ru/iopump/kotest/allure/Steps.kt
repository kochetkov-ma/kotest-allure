package ru.iopump.kotest.allure

import io.qameta.allure.Attachment
import io.qameta.allure.Step

@Step("Allure aspectj step annotation on method 'step1'. With param {param}")
fun step1(param: String = "default") {
    println("Execute step-1 with param $param")
}

@Step("Allure aspectj step annotation on method 'step2'")
fun step2() {
    println("Execute step-2")
    stepNested()
}

@Step("Allure aspectj nested step annotation on method 'stepNested'")
fun stepNested() {
    println("Execute nested step")
}

@Step("Allure aspectj step throw exception1")
fun stepException1() {
    throw AssertionError("Step error1")
}

@Step("Allure aspectj step throw exception2")
fun stepException2() {
    throw AssertionError("Step error2")
}

@Attachment("Allure Attachment created with param {param}")
@JvmName("attachText")
fun attachText(param: String = "default"): String = "This is allure Attachment $param"