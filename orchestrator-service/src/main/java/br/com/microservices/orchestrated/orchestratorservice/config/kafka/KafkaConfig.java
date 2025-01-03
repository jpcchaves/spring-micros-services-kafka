package br.com.microservices.orchestrated.orchestratorservice.config.kafka;

import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import java.util.*;
import java.util.stream.Collectors;
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

@EnableKafka
@Configuration
@Slf4j
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;

  @Value("${spring.kafka.consumer.auto-offset-reset}")
  private String autoOffsetReset;

  private final ApplicationContext applicationContext;

  private static final int PARTITION_COUNT = 1;
  private static final int REPLICA_COUNT = 1;

  public KafkaConfig(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

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
  public List<NewTopic> buildTopics() {
    BeanDefinitionRegistry registry =
        (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();

    return Arrays.stream(ETopics.values())
        .map(topic -> createAndRegisterTopic(topic, registry))
        .collect(Collectors.toList());
  }

  private Map<String, Object> consumerProps() {
    return Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG, groupId,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
  }

  private Map<String, Object> producerProps() {
    return Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
  }

  private NewTopic createAndRegisterTopic(ETopics topic, BeanDefinitionRegistry registry) {
    String topicName = topic.getTopic();

    log.info("Creating topic: {}", topicName);

    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(NewTopic.class);
    builder
        .addConstructorArgValue(topicName)
        .addConstructorArgValue(PARTITION_COUNT)
        .addConstructorArgValue(REPLICA_COUNT);

    registry.registerBeanDefinition(topicName, builder.getBeanDefinition());

    return TopicBuilder.name(topicName).partitions(PARTITION_COUNT).replicas(REPLICA_COUNT).build();
  }
}
