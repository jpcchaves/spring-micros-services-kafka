package br.com.microservices.orchestrated.orderservice.config.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.util.ReflectionUtils;

@EnableKafka
@Configuration
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;

  @Value("${spring.kafka.consumer.auto-offset-reset}")
  private String autoOffsetReset;

  @Value("${spring.kafka.topic.start-saga}")
  private String startSagaTopic;

  @Value("${spring.kafka.topic.notify-ending}")
  private String notifyEndingTopic;

  private static final int PARTITION_COUNT = 1;
  private static final int REPLICA_COUNT = 1;

  @Bean
  public ConsumerFactory<String, String> consumerFactory() {

    return new DefaultKafkaConsumerFactory<>(consumerProps());
  }

  @Bean
  public ProducerFactory<String, String> producerFactory() {

    return new DefaultKafkaProducerFactory<>(producerProps());
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate(
      ProducerFactory<String, String> producerFactory) {

    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public Set<NewTopic> kafkaTopics() {
    Map<String, String> topicProperties = extractTopicProperties();
    return topicProperties.values().stream().map(this::buildTopic).collect(Collectors.toSet());
  }

  private Map<String, Object> consumerProps() {

    Map<String, Object> props = new HashMap<>();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

    return props;
  }

  private Map<String, Object> producerProps() {

    Map<String, Object> props = new HashMap<>();

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    return props;
  }

  private NewTopic buildTopic(String name) {
    return TopicBuilder.name(name).partitions(PARTITION_COUNT).replicas(REPLICA_COUNT).build();
  }

  private Map<String, String> extractTopicProperties() {
    Map<String, String> topics = new HashMap<>();

    ReflectionUtils.doWithFields(
            this.getClass(),
            field -> {
              field.setAccessible(true);
              Object value = field.get(this);

              if (value instanceof String) {
                topics.put(field.getName(), (String) value);
              }
            },
            field ->
                    field.isAnnotationPresent(Value.class)
                            && field.getAnnotation(Value.class).value().startsWith("${spring.kafka.topic.}"));

    return topics;
  }
}
