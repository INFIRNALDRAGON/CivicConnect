package com.civicconnect.backend.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.reporting.ReportingService;
import com.civicconnect.backend.request.RequestStatus;

/** Management views (FR-013 to FR-016). Dates are ISO yyyy-MM-dd; {@code to} is inclusive. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportingService reporting;

    public ReportController(ReportingService reporting) {
        this.reporting = reporting;
    }

    @GetMapping("/status-counts")
    public Map<RequestStatus, Long> statusCounts(@AuthenticationPrincipal AuthenticatedUser actor,
                                                 @RequestParam(required = false) String from,
                                                 @RequestParam(required = false) String to,
                                                 @RequestParam(required = false) Long categoryId) {
        return reporting.countsByStatus(actor, startOf(from), endOf(to), categoryId);
    }

    @GetMapping("/overdue")
    public List<ReportingService.OverdueItem> overdue(@AuthenticationPrincipal AuthenticatedUser actor,
                                                      @RequestParam(required = false) Long categoryId) {
        return reporting.overdue(actor, categoryId);
    }

    @GetMapping("/resolution-times")
    public List<ReportingService.ResolutionTime> resolutionTimes(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                 @RequestParam(required = false) String from,
                                                                 @RequestParam(required = false) String to) {
        return reporting.averageResolutionTimes(actor, startOf(from), endOf(to));
    }

    private static Instant startOf(String date) {
        return date == null || date.isBlank() ? null : parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static Instant endOf(String date) {
        return date == null || date.isBlank() ? null
                : parse(date).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static LocalDate parse(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Dates must look like 2026-10-12");
        }
    }
}
