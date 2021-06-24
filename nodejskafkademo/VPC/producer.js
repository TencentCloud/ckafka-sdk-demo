const Kafka = require('node-rdkafka');
const config = require('./setting');
console.log("features:" + Kafka.features);
console.log(Kafka.librdkafkaVersion);

var producer = new Kafka.Producer({
    'api.version.request': 'true',
    // 设置入口服务，请通过控制台获取对应的服务地址。
    'bootstrap.servers': config['bootstrap_servers'],
    'dr_cb': true,
    'dr_msg_cb': true,

    // 请求发生错误时重试次数，建议将该值设置为大于0，失败重试最大程度保证消息不丢失
    // 'retries': '0',
    // 发送请求失败时到下一次重试请求之间的时间
    // "retry.backoff.ms": 100,
    // producer 网络请求的超时时间。
    // 'socket.timeout.ms': 6000,
});

var connected = false

producer.setPollInterval(100);

producer.connect();


producer.on('ready', function() {
  connected = true
  console.log("connect ok")
});

producer.on("disconnected", function() {
  connected = false;
  producer.connect();
})

producer.on('event.log', function(event) {
      console.log("event.log", event);
});

producer.on("error", function(error) {
    console.log("error:" + error);
});

function produce() {
  try {
    producer.produce(
      config['topic_name'],   
      null,      
      new Buffer('Hello CKafka Default'),      
      null,   
      Date.now()
    );
  } catch (err) {
    console.error('Error occurred when sending message(s)');
    console.error(err);
  }

}

producer.on('delivery-report', function(err, report) {
    console.log("delivery-report: producer ok");
});

producer.on('event.error', function(err) {
    console.error('event.error:' + err);
})

setInterval(produce, 1000, "Interval");

