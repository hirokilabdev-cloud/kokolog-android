package com.kokolog.app

import org.junit.Test

/** Android Studio からテスト実行する入口（JUnit4）。 */
class ReportAnalyzerTest {
    @Test
    fun regressionSuite() {
        ReportAnalyzerSelfTest.runAll()
    }
}
