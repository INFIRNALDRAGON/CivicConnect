package com.civicconnect.backend.reporting;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.request.Category;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;

@Service
public class ReportingService {

    public static final int DEFAULT_RANGE_DAYS = 7;

    private final RequestService requests;
    private final Clock clock;

    public ReportingService(RequestService requests, Clock clock) {
        this.requests = requests;
        this.clock = clock;
    }

    public Map<RequestStatus, Long> countsByStatus(AuthenticatedUser actor, Instant from, Instant to, UUID categoryId) {
        Instant end = rangeEnd(to);
        Instant start = from == null ? end.minus(Duration.ofDays(DEFAULT_RANGE_DAYS)) : from;
        if (start.isAfter(end)) {
            throw new ValidationException("The start of the date range must not be after its end");
        }
        Map<RequestStatus, Long> counts = emptyCounts();
        requests.findAllForReporting(actor).stream()
                .filter(r -> categoryId == null || categoryId.equals(r.getCategoryId()))
                .filter(r -> !r.getCreatedAt().isBefore(start) && r.getCreatedAt().isBefore(end))
                .forEach(r -> counts.merge(r.getStatus(), 1L, Long::sum));
        return counts;
    }

    public List<OverdueItem> overdue(AuthenticatedUser actor, UUID categoryId) {
        Instant now = clock.instant();
        Map<UUID, Duration> thresholds = requests.overdueThresholds();
        return requests.findAllForReporting(actor).stream()
                .filter(r -> categoryId == null || categoryId.equals(r.getCategoryId()))
                .filter(r -> requests.isOverdue(r, now, thresholds))
                .sorted(Comparator.comparing(ServiceRequest::getCreatedAt))
                .map(r -> new OverdueItem(r.getId(), r.getReferenceNumber(), r.getCategoryId(),
                        r.getStatus(), Duration.between(r.getCreatedAt(), now).toHours()))
                .toList();
    }

    public List<ResolutionTime> averageResolutionTimes(AuthenticatedUser actor, Instant from, Instant to) {
        Instant end = rangeEnd(to);
        Instant start = from == null ? end.minus(Duration.ofDays(DEFAULT_RANGE_DAYS)) : from;
        Map<UUID, List<ServiceRequest>> byCategory = requests.findAllForReporting(actor).stream()
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

    public Summary summary(AuthenticatedUser actor, Instant from, Instant to, UUID categoryId) {
        Instant now = clock.instant();
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? now.plusMillis(1) : to;
        if (start.isAfter(end)) {
            throw new ValidationException("The start of the date range must not be after its end");
        }
        Map<UUID, Duration> thresholds = requests.overdueThresholds();
        Map<UUID, String> names = requests.categoryNames();

        List<ServiceRequest> inRange = requests.findAllForReporting(actor).stream()
                .filter(r -> categoryId == null || categoryId.equals(r.getCategoryId()))
                .filter(r -> !r.getCreatedAt().isBefore(start) && r.getCreatedAt().isBefore(end))
                .toList();

        Map<RequestStatus, Long> totals = emptyCounts();
        inRange.forEach(r -> totals.merge(r.getStatus(), 1L, Long::sum));

        long open = inRange.stream().filter(r -> r.getStatus().isOpen()).count();
        long overdue = inRange.stream().filter(r -> requests.isOverdue(r, now, thresholds)).count();

        List<CategorySummary> byCategory = requests.allCategories().stream()
                .filter(c -> categoryId == null || categoryId.equals(c.id()))
                .sorted(Comparator.comparing(Category::name))
                .map(c -> categorySummary(c, inRange, now, thresholds))
                .filter(c -> c.total() > 0)
                .toList();

        List<ReportRequest> listed = inRange.stream()
                .sorted(Comparator.comparing(ServiceRequest::getCreatedAt).reversed())
                .map(r -> new ReportRequest(r.getId(), r.getReferenceNumber(),
                        names.getOrDefault(r.getCategoryId(), "Unknown"), r.getStatus(),
                        r.getCreatedAt().toString(), requests.isOverdue(r, now, thresholds)))
                .toList();

        return new Summary(inRange.size(), totals, open, overdue, byCategory, listed);
    }

    private CategorySummary categorySummary(Category c, List<ServiceRequest> inRange, Instant now,
                                            Map<UUID, Duration> thresholds) {
        List<ServiceRequest> items = inRange.stream().filter(r -> c.id().equals(r.getCategoryId())).toList();
        List<ServiceRequest> finished = items.stream().filter(r -> r.getResolvedAt() != null).toList();
        Double average = finished.isEmpty() ? null
                : Math.round(finished.stream()
                        .mapToDouble(r -> Duration.between(r.getCreatedAt(), r.getResolvedAt()).toMinutes() / 60.0)
                        .average().orElse(0) * 10) / 10.0;
        return new CategorySummary(c.id(), c.name(), items.size(),
                items.stream().filter(r -> r.getStatus().isOpen()).count(),
                items.stream().filter(r -> requests.isOverdue(r, now, thresholds)).count(),
                items.stream().filter(r -> r.getStatus() == RequestStatus.RESOLVED).count(),
                items.stream().filter(r -> r.getStatus() == RequestStatus.CLOSED).count(),
                average);
    }

    private static Map<RequestStatus, Long> emptyCounts() {
        Map<RequestStatus, Long> counts = new EnumMap<>(RequestStatus.class);
        for (RequestStatus s : RequestStatus.values()) {
            counts.put(s, 0L);
        }
        return counts;
    }

    private Instant rangeEnd(Instant to) {
        return to == null ? clock.instant().plusMillis(1) : to;
    }

    public record OverdueItem(UUID requestId, String referenceNumber, UUID categoryId,
                              RequestStatus status, long ageHours) {
    }

    public record ResolutionTime(UUID categoryId, double averageHours, int sampleSize) {
    }

    public record CategorySummary(UUID categoryId, String categoryName, long total, long open, long overdue,
                                  long resolved, long closed, Double avgResolutionHours) {
    }

    public record ReportRequest(UUID id, String referenceNumber, String categoryName, RequestStatus status,
                                String createdAt, boolean overdue) {
    }

    public record Summary(long total, Map<RequestStatus, Long> totals, long openCount, long overdueCount,
                          List<CategorySummary> byCategory, List<ReportRequest> requests) {
    }
}
