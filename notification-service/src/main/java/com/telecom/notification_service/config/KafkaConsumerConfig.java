package com.telecom.notification_service.config;

import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

	private static final String DLQ_SUFFIX = "-dlt";

	@Bean(name = "kafkaListenerContainerFactory")
	public ConcurrentKafkaListenerContainerFactory<String, SubscriptionCreatedEvent> kafkaListenerContainerFactory(
			ConsumerFactory<Object, Object> consumerFactory,
			KafkaTemplate<Object, Object> kafkaTemplate
	) {
		ConcurrentKafkaListenerContainerFactory<String, SubscriptionCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.setCommonErrorHandler(defaultErrorHandler(kafkaTemplate));
		return factory;
	}

	@Bean
	public DefaultErrorHandler defaultErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
				kafkaTemplate,
				(record, exception) -> new TopicPartition(record.topic() + DLQ_SUFFIX, record.partition())
		);

		return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
	}

	@Bean
	public NewTopic subscriptionCreatedDlqTopic() {
		return new NewTopic("subscription-created-topic-dlt", 1, (short) 1);
	}
}
