<?php

$setting = require __DIR__ . '/CKafkaSetting.php';

$conf = new RdKafka\Conf();
// 设置入口服务，请通过控制台获取对应的服务地址。
$conf->set('bootstrap.servers', $setting['bootstrap_servers']);

// ---------- 启用 SASL 验证时需要设置 ----------
// SASL 验证机制类型默认选用 PLAIN
$conf->set('sasl.mechanism', 'PLAIN');
// 设置用户名：实例 ID + # + 【用户管理】中配置的用户名
$conf->set('sasl.username', $setting['ckafka_instance_id'] . '#' . $setting['sasl_username']);
// 设置密码：【用户管理】中配置的密码
$conf->set('sasl.password', $setting['sasl_password']);
// 在本地配置 ACL 策略。
$conf->set('security.protocol', 'SASL_PLAINTEXT');
// ---------- 启用 SASL 验证时需要设置 ----------

// Kafka producer 的 ack 有 3 种机制：-1、0、1
// 用户不显示配置时，默认值为1。用户根据自己的业务情况进行设置
$conf->set('acks', '1');
// 请求发生错误时重试次数，建议将该值设置为大于0，失败重试最大程度保证消息不丢失
$conf->set('retries', '0');
// 发送请求失败时到下一次重试请求之间的时间
$conf->set('retry.backoff.ms', 100);
// producer 网络请求的超时时间。
$conf->set('socket.timeout.ms', 6000);
$conf->set('reconnect.backoff.max.ms', 3000);

// 注册发送消息的回调
$conf->setDrMsgCb(
    function ($kafka, $message) {
        echo '【Producer】发送消息：message=' . var_export($message, true) . "\n";
    }
);
// 注册发送消息错误的回调
$conf->setErrorCb(
    function ($kafka, $err, $reason) {
        echo "【Producer】发送消息错误：err=$err reason=$reason \n";
    }
);

$producer = new RdKafka\Producer($conf);
// Debug 时请设置为 LOG_DEBUG
//$producer->setLogLevel(LOG_DEBUG);
$topicConf = new RdKafka\TopicConf();
$topic = $producer->newTopic($setting['topic_name'], $topicConf);
// 生产消息并发送
for ($i = 0; $i < 5; $i++) {
    // RD_KAFKA_PARTITION_UA 让 kafka 自由选择分区
    $topic->produce(RD_KAFKA_PARTITION_UA, 0, "Message $i");
    $producer->poll(0);
}

while ($producer->getOutQLen() > 0) {
    $producer->poll(50);
}

echo "【Producer】消息发送成功\n";
