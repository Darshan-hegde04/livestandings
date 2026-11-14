package com.phinixlabs.livestandings.config;

import com.phinixlabs.livestandings.kafka.ResultEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic matchResultsTopic(
            @Value("${livestandings.kafka.topics.match-results:match-results}")
            String topic
    ) {
        return TopicBuilder
                .name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, ResultEvent> resultsKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.phinixlabs.livestandings.kafka");
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, ResultEvent.class.getName());

        ConsumerFactory<String, ResultEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        props,
                        new StringDeserializer(),
                        new JacksonJsonDeserializer<>(ResultEvent.class, false)
                );

        ConcurrentKafkaListenerContainerFactory<String, ResultEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
