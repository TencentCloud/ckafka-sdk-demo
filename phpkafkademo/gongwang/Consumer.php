<?php

$setting = require __DIR__ . '/CKafkaSetting.php';

$conf = new RdKafka\Conf();
$conf->set('group.id', $setting['group_id']);
$conf->set('bootstrap.servers', $setting['bootstrap_servers']);
// ---------- 非 SASL 验证模式不需要设置 ----------
$conf->set('sasl.mechanism', 'PLAIN');
$conf->set('security.protocol', 'SASL_PLAINTEXT');
$conf->set('sasl.username', $setting['ckafka_instance_id'] . '#' . $setting['sasl_username']);
$conf->set('sasl.password', $setting['sasl_password']);
// ---------- 非 SASL 验证模式不需要设置 ----------

// 使用 Kafka 消费分组机制时，消费者超时时间。当 Broker 在该时间内没有收到消费者的心跳时，
// 认为该消费者故障失败，Broker 发起重新 Rebalance 过程。
$conf->set('session.timeout.ms', 10000);
// 客户端请求超时时间，如果超过这个时间没有收到应答，则请求超时失败
$conf->set('request.timeout.ms', 305000);
// 设置客户端内部重试间隔。
$conf->set('reconnect.backoff.max.ms', 3000);

$topicConf = new RdKafka\TopicConf();
#$topicConf->set('auto.commit.interval.ms', 100);
// offset重置策略，请根据业务场景酌情设置。设置不当可能导致数据消费缺失。
$topicConf->set('auto.offset.reset', 'earliest');
$conf->setDefaultTopicConf($topicConf);

$consumer = new RdKafka\KafkaConsumer($conf);
// Debug 时请设置为 LOG_DEBUG
//$consumer->setLogLevel(LOG_DEBUG);
$consumer->subscribe([$setting['topic_name']]);

$isConsuming = true;
while ($isConsuming) {
    $message = $consumer->consume(10 * 1000);
    switch ($message->err) {
        case RD_KAFKA_RESP_ERR_NO_ERROR:
            echo "【消费者】接收到消息：" . var_export($message, true) . "\n";
            break;
        case RD_KAFKA_RESP_ERR__PARTITION_EOF:
            echo "【消费者】等待信息消息中\n";
            break;
        case RD_KAFKA_RESP_ERR__TIMED_OUT:
            echo "【消费者】等待超时\n";
            $isConsuming = false;
            break;
        default:
            throw new \Exception($message->errstr(), $message->err);
            break;
    }
}
