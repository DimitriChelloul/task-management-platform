package org.dimitri.shared.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {
    @Bean
    NewTopic userCreatedTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.userCreated()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic taskCreatedTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.taskCreated()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic taskCompletedTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.taskCompleted()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic userCreatedDlq(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.userCreated() + ".dlq").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic taskCreatedDlq(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.taskCreated() + ".dlq").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic taskCompletedDlq(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.taskCompleted() + ".dlq").partitions(1).replicas(1).build();
    }
}
