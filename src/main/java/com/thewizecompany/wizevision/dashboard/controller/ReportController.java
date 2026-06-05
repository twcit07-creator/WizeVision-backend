package com.thewizecompany.wizevision.dashboard.controller;

import com.thewizecompany.wizevision.dashboard.dto.AttendanceReportResponse;
import com.thewizecompany.wizevision.dashboard.dto.FinancialReportResponse;
import com.thewizecompany.wizevision.dashboard.dto.LeaveReportResponse;
import com.thewizecompany.wizevision.dashboard.dto.ProjectReportResponse;
import com.thewizecompany.wizevision.dashboard.service.ReportService;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(
        name = "Reports",
        description = "Attendance, project, financial and leave reports"
)
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/attendance")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(
            summary = "Attendance report",
            description = "Employee-wise attendance summary " +
                    "for a date range."
    )
    public ResponseEntity<ApiResponse<AttendanceReportResponse>>
    attendanceReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false)
            UUID departmentId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        reportService.getAttendanceReport(
                                from, to, departmentId
                        )
                )
        );
    }

    @GetMapping("/projects")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN'," +
                    "'PROJECT_MANAGER','FINANCE')"
    )
    @Operation(
            summary = "Project report",
            description = "All projects with financials and status. " +
                    "Filter by status, client, date range."
    )
    public ResponseEntity<ApiResponse<ProjectReportResponse>>
    projectReport(
            @RequestParam(required = false)
            String status,
            @RequestParam(required = false)
            UUID clientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        reportService.getProjectReport(
                                status, clientId, from, to
                        )
                )
        );
    }

    @GetMapping("/financial")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')"
    )
    @Operation(
            summary = "Financial report",
            description = "Monthly revenue breakdown and " +
                    "client-wise billing for a year."
    )
    public ResponseEntity<ApiResponse<FinancialReportResponse>>
    financialReport(
            @RequestParam(
                    defaultValue = "#{T(java.time.Year)" +
                            ".now().getValue()}"
            ) int year) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        reportService.getFinancialReport(year)
                )
        );
    }

    @GetMapping("/leave")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(
            summary = "Leave report",
            description = "Leave utilization by employee " +
                    "and leave type for a year."
    )
    public ResponseEntity<ApiResponse<LeaveReportResponse>>
    leaveReport(
            @RequestParam(
                    defaultValue = "#{T(java.time.Year)" +
                            ".now().getValue()}"
            ) int year,
            @RequestParam(required = false)
            UUID departmentId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        reportService.getLeaveReport(
                                year, departmentId
                        )
                )
        );
    }
}