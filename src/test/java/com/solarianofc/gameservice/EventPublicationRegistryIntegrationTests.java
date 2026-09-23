package com.solarianofc.gameservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The Spring Modulith Event Publication Registry acts as a transactional outbox (D-108): publications are stored in
 * {@code event_publication} within the publishing transaction and completed only by a successful listener, which
 * runs asynchronously (async enabled by the Modulith auto-configuration, D-117). The listeners exist only in this test's own context (D-114).
 */
@IntegrationTest
class EventPublicationRegistryIntegrationTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingListener listener;

    @Test
    void listenerRunsOnAnotherThreadThanThePublisher() {
        UUID id = UUID.randomUUID();
        // Captured here: Awaitility evaluates the condition on its own polling thread.
        String publisherThread = Thread.currentThread().getName();

        publishInTransaction(new SucceedingEvent(id));

        await().atMost(TIMEOUT)
                .untilAsserted(
                        () -> assertThat(listener.threadOf(id)).isNotNull().isNotEqualTo(publisherThread));
    }

    @Test
    void successfulListenerCompletesThePublication() {
        UUID id = UUID.randomUUID();

        publishInTransaction(new SucceedingEvent(id));

        assertPublicationEventually(id, "COMPLETED", true);
    }

    @Test
    void failingListenerLeavesThePublicationIncomplete() {
        UUID id = UUID.randomUUID();

        publishInTransaction(new FailingEvent(id));

        assertPublicationEventually(id, "FAILED", false);
    }

    @Test
    void rolledBackTransactionLeavesNoPublication() {
        UUID id = UUID.randomUUID();

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(new SucceedingEvent(id));
            status.setRollbackOnly();
        });

        assertThat(publications(id)).isEmpty();
    }

    private void publishInTransaction(Object event) {
        transactionTemplate.executeWithoutResult(status -> publisher.publishEvent(event));
    }

    private void assertPublicationEventually(UUID id, String status, boolean completed) {
        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertThat(publications(id))
                        .extracting(row -> row.get("status"), row -> row.get("completed"))
                        .containsExactly(tuple(status, completed)));
    }

    private List<Map<String, Object>> publications(UUID id) {
        return jdbcTemplate.queryForList(
                "SELECT status, completion_date IS NOT NULL AS completed FROM event_publication"
                        + " WHERE serialized_event LIKE ?",
                "%" + id + "%");
    }

    record SucceedingEvent(UUID id) {}

    record FailingEvent(UUID id) {}

    static class RecordingListener {

        private final Map<UUID, String> listenerThreads = new ConcurrentHashMap<>();

        @ApplicationModuleListener
        public void on(SucceedingEvent event) {
            listenerThreads.put(event.id(), Thread.currentThread().getName());
        }

        @ApplicationModuleListener
        public void on(FailingEvent event) {
            throw new IllegalStateException("Deliberate listener failure for " + event.id());
        }

        String threadOf(UUID id) {
            return listenerThreads.get(id);
        }
    }

    @TestConfiguration
    static class ListenerConfiguration {

        @Bean
        RecordingListener recordingListener() {
            return new RecordingListener();
        }
    }
}
