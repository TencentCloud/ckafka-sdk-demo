package com.tencent.tcb.operation.ckafka.plain;

import com.google.common.collect.Lists;
import com.tencent.tcb.operation.ckafka.JavaKafkaConfigurer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.NoOffsetForPartitionException;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;

public class KafkaSASLAutoOffsetConsumerDemo {

    public static void main(String args[]) {
        //设置JAAS配置文件的路径。
        JavaKafkaConfigurer.configureSaslPlain();

        //加载kafka.properties。
        Properties kafkaProperties = JavaKafkaConfigurer.getKafkaProperties();

        Properties props = new Properties();
        //设置接入点，请通过控制台获取对应Topic的接入点。
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getProperty("bootstrap.servers"));

        //接入协议。
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        //Plain方式。
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        //两次Poll之间的最大允许间隔。
        //消费者超过该值没有返回心跳，服务端判断消费者处于非存活状态，服务端将消费者从Consumer Group移除并触发Rebalance，默认30s。
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        //每次Poll的最大数量。
        //注意该值不要改得太大，如果Poll太多数据，而不能在下次Poll之前消费完，则会触发一次负载均衡，产生卡顿。
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 30);
        //消息的反序列化方式。
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        //当前消费实例所属的消费组，请在控制台申请之后填写。
        //属于同一个组的消费实例，会负载消费消息。
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getProperty("group.id"));

        //消费offset的位置。注意！如果auto.offset.reset=none这样设置，消费组在第一次消费的时候 就会报错找不到offset，第一次这时候就需要在catch里手动设置offset。
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "none"); // 消费offset的位置
        //构造消费对象，也即生成一个消费实例。
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
        //设置消费组订阅的Topic，可以订阅多个。
        //如果GROUP_ID_CONFIG是一样，则订阅的Topic也建议设置成一样。
        List<String> subscribedTopics = new ArrayList<String>();
        //如果需要订阅多个Topic，则在这里添加进去即可。
        //每个Topic需要先在控制台进行创建。
        String topicStr = kafkaProperties.getProperty("topic");
        String[] topics = topicStr.split(",");
        for (String topic : topics) {
            subscribedTopics.add(topic.trim());
        }
        consumer.subscribe(subscribedTopics);
        //循环消费消息。
        while (true) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                //必须在下次Poll之前消费完这些数据, 且总耗时不得超过SESSION_TIMEOUT_MS_CONFIG。 建议开一个单独的线程池来消费消息，然后异步返回结果。
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(
                            String.format("Consume partition:%d offset:%d", record.partition(), record.offset()));
                }
            } catch (NoOffsetForPartitionException e) {//当auto.offset.reset设置为none时，需要捕获异常
                System.out.println(e.getMessage());
                //当auto.offset.reset设置为none时，需要捕获异常 自己设置offset。您可以根据自身业务情况选择以下方式中的其中一种。
                //e.g 1 :指定offset, 这里需要自己维护offset，方便重试。
                Map<Integer, Long> partitionBeginOffsetMap = getPartitionOffset(consumer, topicStr, true);
                Map<Integer, Long> partitionEndOffsetMap = getPartitionOffset(consumer, topicStr, false);
                consumer.seek(new TopicPartition(topicStr, 0), 0);

                //e.g 2:从头开始消费
                consumer.seekToBeginning(Lists.newArrayList(new TopicPartition(topicStr, 0)));

                //e.g 3:指定offset为最近可用的offset。
                consumer.seekToEnd(Lists.newArrayList(new TopicPartition(topicStr, 0)));

                //e.g 4: 根据时间戳获取offset，就是根据时间戳去设置offset。比如重置到5分钟前的offset
                Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
                Long value = Instant.now().minus(300, ChronoUnit.SECONDS).toEpochMilli();
                timestampsToSearch.put(new TopicPartition(topicStr, 0), value);
                Map<TopicPartition, OffsetAndTimestamp> topicPartitionOffsetAndTimestampMap = consumer
                        .offsetsForTimes(timestampsToSearch);
                for (Entry<TopicPartition, OffsetAndTimestamp> entry : topicPartitionOffsetAndTimestampMap
                        .entrySet()) {
                    TopicPartition topicPartition = entry.getKey();
                    OffsetAndTimestamp entryValue = entry.getValue();
                    consumer.seek(topicPartition, entryValue.offset()); // 指定offset, 这里需要自己维护offset，方便重试。
                }
            }
        }
    }

    /**
     * 获取topic的最早、最进的offset
     * @param consumer
     * @param topicStr
     * @param beginOrEnd true begin; false end
     * @return
     */
    private static Map<Integer, Long> getPartitionOffset(KafkaConsumer<String, String> consumer, String topicStr,
            boolean beginOrEnd) {
        Collection<PartitionInfo> partitionInfos = consumer.partitionsFor(topicStr);
        List<TopicPartition> tp = new ArrayList<>();
        Map<Integer, Long> map = new HashMap<>();
        partitionInfos.forEach(str -> {
            tp.add(new TopicPartition(topicStr, str.partition()));

        });
        Map<TopicPartition, Long> topicPartitionLongMap;
        if (beginOrEnd) {
            topicPartitionLongMap = consumer.beginningOffsets(tp);
        } else {
            topicPartitionLongMap = consumer.endOffsets(tp);
        }
        topicPartitionLongMap.forEach((key, beginOffset) -> {
            int partition = key.partition();
            map.put(partition, beginOffset);
        });
        return map;
    }

}