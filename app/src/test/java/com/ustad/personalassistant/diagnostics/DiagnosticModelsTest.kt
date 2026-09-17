package com.ustad.personalassistant.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticModelsTest {
    @Test fun errorsDominateOverallStatus() {
        val report = DiagnosticReport(1L, listOf(
            DiagnosticFinding(DiagnosticCategory.BATTERY, DiagnosticStatus.HEALTHY, "Battery", "ok"),
            DiagnosticFinding(DiagnosticCategory.NETWORK, DiagnosticStatus.ERROR, "Network", "failed")
        ))
        assertEquals(DiagnosticStatus.ERROR, report.overallStatus)
    }

    @Test fun warningsDominateHealthyFindings() {
        val report = DiagnosticReport(1L, listOf(
            DiagnosticFinding(DiagnosticCategory.BATTERY, DiagnosticStatus.HEALTHY, "Battery", "ok"),
            DiagnosticFinding(DiagnosticCategory.STORAGE, DiagnosticStatus.WARNING, "Storage", "low")
        ))
        assertEquals(DiagnosticStatus.WARNING, report.overallStatus)
    }

    @Test fun allUnavailableIsUnknown() {
        val report = DiagnosticReport(1L, listOf(
            DiagnosticFinding(DiagnosticCategory.NETWORK, DiagnosticStatus.UNAVAILABLE, "Network", "n/a"),
            DiagnosticFinding(DiagnosticCategory.APP, DiagnosticStatus.UNKNOWN, "App", "unknown")
        ))
        assertEquals(DiagnosticStatus.UNKNOWN, report.overallStatus)
    }

    @Test fun healthyReportIsHealthy() {
        val report = DiagnosticReport(1L, listOf(
            DiagnosticFinding(DiagnosticCategory.SYSTEM, DiagnosticStatus.HEALTHY, "System", "ok")
        ))
        assertTrue(report.overallStatus == DiagnosticStatus.HEALTHY)
    }
}
