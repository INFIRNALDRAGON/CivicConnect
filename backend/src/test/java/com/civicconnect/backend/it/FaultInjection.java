package com.civicconnect.backend.it;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import com.civicconnect.backend.status.StatusChangedEvent;

@TestConfiguration(proxyBeanMethods = false)
public class FaultInjection {

    static final AtomicBoolean FAIL_STATUS_EVENTS = new AtomicBoolean(false);

    @Bean
    FailingListener failingListener() {
        return new FailingListener();
    }

    static class FailingListener {

        @EventListener
        void onStatusChanged(StatusChangedEvent event) {
            if (FAIL_STATUS_EVENTS.get()) {
                throw new IllegalStateException("injected failure while recording the status change");
            }
        }
    }
}
