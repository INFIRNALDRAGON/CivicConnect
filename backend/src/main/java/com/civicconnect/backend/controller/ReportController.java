package com.civicconnect.backend.controller;

import java.time.Instant;
import java.time.LocalDate;
<<<<<<< Updated upstream
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
=======
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
>>>>>>> Stashed changes

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.reporting.ReportingService;
import com.civicconnect.backend.request.RequestStatus;

<<<<<<< Updated upstream
/** Management views (FR-013 to FR-016). Dates are ISO yyyy-MM-dd; {@code to} is inclusive. */
=======
>>>>>>> Stashed changes
@RestController
@RequestMapping("/api/reports")
public class ReportController {

<<<<<<< Updated upstream
=======
    private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");

>>>>>>> Stashed changes
    private final ReportingService reporting;

    public ReportController(ReportingService reporting) {
        this.reporting = reporting;
    }

<<<<<<< Updated upstream
=======
    @GetMapping("/summary")
    public ReportingService.Summary summary(@AuthenticationPrincipal AuthenticatedUser actor,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to,
                                            @RequestParam(required = false) UUID categoryId) {
        return reporting.summary(actor, startOf(from), endOf(to), categoryId);
    }

>>>>>>> Stashed changes
    @GetMapping("/status-counts")
    public Map<RequestStatus, Long> statusCounts(@AuthenticationPrincipal AuthenticatedUser actor,
                                                 @RequestParam(required = false) String from,
                                                 @RequestParam(required = false) String to,
<<<<<<< Updated upstream
                                                 @RequestParam(required = false) Long categoryId) {
=======
                                                 @RequestParam(required = false) UUID categoryId) {
>>>>>>> Stashed changes
        return reporting.countsByStatus(actor, startOf(from), endOf(to), categoryId);
    }

    @GetMapping("/overdue")
    public List<ReportingService.OverdueItem> overdue(@AuthenticationPrincipal AuthenticatedUser actor,
<<<<<<< Updated upstream
                                                      @RequestParam(required = false) Long categoryId) {
=======
                                                      @RequestParam(required = false) UUID categoryId) {
>>>>>>> Stashed changes
        return reporting.overdue(actor, categoryId);
    }

    @GetMapping("/resolution-times")
    public List<ReportingService.ResolutionTime> resolutionTimes(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                 @RequestParam(required = false) String from,
                                                                 @RequestParam(required = false) String to) {
        return reporting.averageResolutionTimes(actor, startOf(from), endOf(to));
    }

    private static Instant startOf(String date) {
<<<<<<< Updated upstream
        return date == null || date.isBlank() ? null : parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static Instant endOf(String date) {
        return date == null || date.isBlank() ? null
                : parse(date).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
=======
        return date == null || date.isBlank() ? null : parse(date).atStartOfDay(SAST).toInstant();
    }

    private static Instant endOf(String date) {
        return date == null || date.isBlank() ? null : parse(date).plusDays(1).atStartOfDay(SAST).toInstant();
>>>>>>> Stashed changes
    }

    private static LocalDate parse(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Dates must look like 2026-10-12");
        }
    }
}
