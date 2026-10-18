package com.civicconnect.backend.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.civicconnect.backend.it.Api.Response;

class ConcurrencyIT extends DatabaseIntegrationTest {

    @Test
    void twoStaffAcceptingTheSameRequestGiveOneWinnerAndOneConflict() throws Exception {
        Api api = api();
        String requester = api.login("req1@civicconnect.test");
        String first = api.login("staff1@civicconnect.test");
        String second = api.login("staff3@civicconnect.test");
        String category = categoryId("IT support");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 5; round++) {
                Response created = api.createRequest(requester, category, unique("Concurrency round " + round));
                String id = created.text("id");
                long version = created.number("version");
                CyclicBarrier gate = new CyclicBarrier(2);

                List<Callable<Integer>> attempts = List.of(
                        () -> { gate.await(); return api.assign(first, id, version).status(); },
                        () -> { gate.await(); return api.assign(second, id, version).status(); });
                List<Integer> codes = new ArrayList<>();
                for (Future<Integer> f : pool.invokeAll(attempts)) {
                    codes.add(f.get());
                }
                Collections.sort(codes);

                assertEquals(List.of(200, 409), codes, "round " + round);
                assertEquals(1, count("SELECT count(*) FROM status_history WHERE request_id = ?::uuid"
                        + " AND new_status = 'ASSIGNED'", id));
                assertEquals("ASSIGNED", queryOne("SELECT status FROM service_requests WHERE id = ?::uuid", id));
                assertEquals(version + 1, count("SELECT version FROM service_requests WHERE id = ?::uuid", id));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void manyRequestersSubmittingTogetherAllGetDistinctReferences() throws Exception {
        Api api = api();
        String requester = api.login("req2@civicconnect.test");
        String category = categoryId("Other");
        int submissions = 20;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> work = new ArrayList<>();
            for (int i = 0; i < submissions; i++) {
                int n = i;
                work.add(() -> {
                    Response r = api.createRequest(requester, category, unique("Parallel submission " + n));
                    assertEquals(201, r.status());
                    return r.text("referenceNumber");
                });
            }
            List<String> references = new ArrayList<>();
            for (Future<String> f : pool.invokeAll(work)) {
                references.add(f.get());
            }
            assertEquals(submissions, references.stream().distinct().count());
        } finally {
            pool.shutdownNow();
        }
    }
}
