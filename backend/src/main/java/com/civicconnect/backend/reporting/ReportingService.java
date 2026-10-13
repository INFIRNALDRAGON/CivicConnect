package com.civicconnect.backend.reporting;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;

/**
 * Read-only management reporting (FR-013 to FR-016). Managers only.
 * Returns aggregates and request references, never requester personal fields (ASR-02 tension, NFR-003).
 */
@Service
public class ReportingService {

    public static final int DEFAULT_RANGE_DAYS = 7;

    private final RequestService requests;
    private final Clock clock;
    private final Duration overdueAfter;

    /**
     * configurable value for every category. Change it with civicconnect.reporting.overdue-after-hours.
     @param requests the request being looked at
     @param clock the time of the request
     @param overdueAfterHours the amount of time it takes a request to be considered overdue
     */
    public ReportingService(RequestService requests, Clock clock,
                            @Value("${civicconnect.reporting.overdue-after-hours:72}") long overdueAfterHours) {
        this.requests = requests;
        this.clock = clock;
        this.overdueAfter = Duration.ofHours(overdueAfterHours);
    }

    /** FR-013 / FR-015: counts by status for requests created in [from, to). Defaults to the last 7 days. 
     *@param actor
     *@param from
     *@param to
     *@param categoryId
     *@return
     */
    public Map<RequestStatus, Long> countsByStatus(AuthenticatedUser actor, Instant from, Instant to, Long categoryId) {
        Instant end = rangeEnd(to);
        Instant start = from == null ? end.minus(Duration.ofDays(DEFAULT_RANGE_DAYS)) : from;
        if (start.isAfter(end)) {
            throw new ValidationException("The start of the date range must not be after its end");
        }

        Map<RequestStatus, Long> counts = new EnumMap<>(RequestStatus.class);
        for (RequestStatus s : RequestStatus.values()) {
            counts.put(s, 0L);
        }
        requests.findAllForReporting(actor).stream()
                .filter(r -> categoryId == null || categoryId.equals(r.getCategoryId()))
                .filter(r -> !r.getCreatedAt().isBefore(start) && r.getCreatedAt().isBefore(end))
                .forEach(r -> counts.merge(r.getStatus(), 1L, Long::sum));
        return counts;
    }

    /** FR-014: open requests older than the threshold. Oldest first. References only. 
     *@param actor the user responsible for making it overdue
     *@param categoryId the category 
     *@return
     */
    public List<OverdueItem> overdue(AuthenticatedUser actor, Long categoryId) {
        Instant now = clock.instant();
        return requests.findAllForReporting(actor).stream()
                .filter(r -> r.getStatus().isOpen())
                .filter(r -> categoryId == null || categoryId.equals(r.getCategoryId()))
                .filter(r -> Duration.between(r.getCreatedAt(), now).compareTo(overdueAfter) > 0)
                .sorted(Comparator.comparing(ServiceRequest::getCreatedAt))
                .map(r -> new OverdueItem(r.getId(), r.getReferenceNumber(), r.getCategoryId(),
                        r.getStatus(), Duration.between(r.getCreatedAt(), now).toHours()))
                .toList();
    }

    /** FR-016: average hours from submission to resolution per category, for requests resolved in the range. 
     *@param actor
     *@param from
     *@param to
     *@return
     */
    public List<ResolutionTime> averageResolutionTimes(AuthenticatedUser actor, Instant from, Instant to) {
        Instant end = rangeEnd(to);
        Instant start = from == null ? end.minus(Duration.ofDays(DEFAULT_RANGE_DAYS)) : from;

        Map<Long, List<ServiceRequest>> byCategory = requests.findAllForReporting(actor).stream()
                .filter(r -> r.getResolvedAt() != null)
                .filter(r -> !r.getResolvedAt().isBefore(start) && r.getResolvedAt().isBefore(end))
                .collect(Collectors.groupingBy(ServiceRequest::getCategoryId, TreeMap::new, Collectors.toList()));

        return byCategory.entrySet().stream().map(e -> {
            double avgHours = e.getValue().stream()
                    .mapToLong(r -> Duration.between(r.getCreatedAt(), r.getResolvedAt()).toMinutes())
                    .average().orElse(0) / 60.0;
            return new ResolutionTime(e.getKey(), Math.round(avgHours * 10) / 10.0, e.getValue().size());
        }).toList();
    }

    /** Ranges are [from, to). With no end given, "now" itself must be included, so step one millisecond past it. */
    private Instant rangeEnd(Instant to) {
        return to == null ? clock.instant().plusMillis(1) : to;
    }

    public record OverdueItem(Long requestId, String referenceNumber, Long categoryId,
                              RequestStatus status, long ageHours) {
    }

    public record ResolutionTime(Long categoryId, double averageHours, int sampleSize) {
    }
}
