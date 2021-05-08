const Kafka = require('node-rdkafka');
const config = require('./setting');
console.log(Kafka.features);
console.log(Kafka.librdkafkaVersion);
console.log(config)

var consumer = new Kafka.KafkaConsumer({
    'api.version.request': 'true',
    // 设置入口服务，请通过控制台获取对应的服务地址。
    'bootstrap.servers': config['bootstrap_servers'],
    'group.id' : config['group_id'],

    // 使用 Kafka 消费分组机制时，消费者超时时间。当 Broker 在该时间内没有收到消费者的心跳时，
    // 认为该消费者故障失败，Broker 发起重新 Rebalance 过程。
    // 'session.timeout.ms': 10000,
    // 客户端请求超时时间，如果超过这个时间没有收到应答，则请求超时失败
    // 'metadata.request.timeout.ms': 305000,

});

consumer.connect();

consumer.on('ready', function() {
  console.log("connect ok");
  consumer.subscribe([config['topic_name']]);
  consumer.consume();
})

consumer.on('data', function(data) {
  console.log(data);
});


consumer.on('event.log', function(event) {
      console.log("event.log", event);
});

consumer.on('error', function(error) {
    console.log("error:" + error);
});

consumer.on('event', function(event) {
        console.log("event:" + event);
});

