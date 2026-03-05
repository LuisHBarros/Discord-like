package com.luishbarros.discord_like.shared.adapters.messaging;

import brave.Tracing;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaTracingInterceptor implements ProducerListener<Object, Object> {

    private final Tracing tracing;

    public KafkaTracingInterceptor(Tracing tracing) {
        this.tracing = tracing;
    }

    @Override
    public void onSuccess(ProducerRecord<Object, Object> record,
                       RecordMetadata metadata) {
        // Injetar headers de tracing
        var traceContext = tracing.currentTraceContext();
        var context = traceContext.get();
        if (context != null) {
            record.headers().add("traceId", context.traceIdString().getBytes());
            record.headers().add("spanId", context.spanIdString().getBytes());
        }
    }
}
