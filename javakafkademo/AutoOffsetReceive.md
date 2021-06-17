

## auto.offset.reset 使用说明

#### 什么是auto.offset.reset

该参数规定了当无法取到消费分区的位移时从何处开始消费。

- 当 Broker 端没有 offset（如第一次消费或 offset 超过7天过期）时如何初始化 offset，
- 当客户端收到 OFFSET_OUT_OF_RANGE 错误时，就根据设置的auto.offset.reset策略去重置Offset。



#### 什么时候会出现OFFSET_OUT_OF_RANGE

​		该错误表示客户端提交的offset不在服务端允许的offset范围之内。比如topicA的分区1的LogStartOffset为100，LogEndOffset为300。此时如果客户端提交的offset小于100或者大于300，服务端就会返回该错误。此时就会进行offset重置。以下情况可能会导致客户端提交错误的offset：

1. 客户端保存了offset，然后一段时间不消费了。topic设置了保留时间，当过了保留时间后，offset在服务端已经被删除了，即发生了日志滚动，此时客户端再提交删除了的offset，就会发生该错误。

2. 因为sdk bug，网络丢包问题等(这种场景很多，没有一个具体的场景列表)问题，导致客户端提交了异常的offset，也会触发该错误。

3. 服务端有未同步副本，此时发生了leader切换，触发了follower副本的截断，此时如果客户端提交的offset是在截断的范围之内，也会触发这个错误。



#### 使用背景

不希望发生offset自动重置的情况，因为业务不允许发生大规模的重复消费。

注意：消费组在第一次消费的时候 就会报错找不到offset，第一次这时候就需要在catch里手动设置offset。

#### 使用说明

auto.offset.reset设置为None以后，可以避免offset自动重置的问题。但是当增加分区的时候，因为关闭了自动重置机制，客户端不知道新的分区要从哪里开始消费，会抛出异常，需要人工去设置消费分组offset，并消费。



#### 使用方式

这里消费者在消费时，consumer设置auto.offset.reset=none， 捕获NoOffsetForPartitionException异常，再catch里自己设置offset。您可以根据自身业务情况选择以下方式中的其中一种。

1. 指定offset, 这里需要自己维护offset，方便重试。
2. 指定从头开始消费。
3. 指定offset为最近可用的offset。
4. 根据时间戳获取offset，设置offset。

**具体demo代码如下：**

```
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

public class KafkaPlainConsumerDemo {

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
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "none");
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
            } catch (NoOffsetForPartitionException e) {
                System.out.println(e.getMessage());

                //当auto.offset.reset设置为 none时，需要捕获异常 自己设置offset。您可以根据自身业务情况选择以下方式中的其中一种。
                //e.g 1 :指定offset, 这里需要自己维护offset，方便重试。
                Map<Integer, Long> partitionBeginOffsetMap = getPartitionOffset(consumer, topicStr, true);
                Map<Integer, Long> partitionEndOffsetMap = getPartitionOffset(consumer, topicStr, false);
                consumer.seek(new TopicPartition(topicStr, 0), 0);

                //e.g 2:从头开始消费
                consumer.seekToBeginning(Lists.newArrayList(new TopicPartition(topicStr, 0)));

                //e.g 3:指定offset为最近可用的offset。
                consumer.seekToEnd(Lists.newArrayList(new TopicPartition(topicStr, 0)));

                //e.g 4: 根据时间戳获取offset，就是根据时间戳去设置offset。比如重置到10分钟前的offset
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
     * 获取topic的最早、最近的offset
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
        partitionInfos.forEach(str -> tp.add(new TopicPartition(topicStr, str.partition())));
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
```
