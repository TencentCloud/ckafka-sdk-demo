const Kafka = require('node-rdkafka');
const config = require('./setting');
console.log(Kafka.features);
console.log(Kafka.librdkafkaVersion);
console.log(config)

var consumer = new Kafka.KafkaConsumer({
    'api.version.request': 'true',
    'bootstrap.servers': config['bootstrap_servers'],
    'security.protocol' : 'SASL_PLAINTEXT',
    'sasl.mechanisms' : 'PLAIN',
    'message.max.bytes': 32000,
    'fetch.message.max.bytes': 32000,
    'max.partition.fetch.bytes': 32000,
    'sasl.username' : config['sasl_plain_username'],
    'sasl.password' : config['sasl_plain_password'],
    'group.id' : config['group_id']
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

