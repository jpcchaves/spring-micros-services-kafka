package br.com.microservices.orchestrated.paymentservice.config.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.util.ReflectionUtils;

@EnableKafka
@Configuration
@Slf4j
@RequiredArgsConstructor
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;

  @Value("${spring.kafka.consumer.auto-offset-reset}")
  private String autoOffsetReset;

  @Value("${spring.kafka.topic.orchestrator}")
  private String orchestratorTopic;

  @Value("${spring.kafka.topic.payment-success}")
  private String paymentSuccess;

  @Value("${spring.kafka.topic.payment-fail}")
  private String paymentFail;

  private final ApplicationContext applicationContext;

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
  public Set<NewTopic> buildTopics() {
    BeanDefinitionRegistry registry =
        (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();

    Map<String, String> topicProperties = extractTopicProperties();

    return topicProperties.values().stream()
        .map(topic -> createAndRegisterTopic(topic, registry))
        .collect(Collectors.toSet());
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
                && field.getAnnotation(Value.class).value().startsWith("${spring.kafka.topic"));

    return topics;
  }

  private NewTopic createAndRegisterTopic(String topicName, BeanDefinitionRegistry registry) {
    log.info("Creating topic: {}", topicName);

    BeanDefinitionBuilder builder =
        BeanDefinitionBuilder.genericBeanDefinition(NewTopic.class)
            .addConstructorArgValue(topicName)
            .addConstructorArgValue(PARTITION_COUNT)
            .addConstructorArgValue(REPLICA_COUNT);

    registry.registerBeanDefinition(topicName, builder.getBeanDefinition());

    return TopicBuilder.name(topicName).partitions(PARTITION_COUNT).replicas(REPLICA_COUNT).build();
  }
}
