const Kafka = require('node-rdkafka');
const config = require('./setting');
console.log("features:" + Kafka.features);
console.log(Kafka.librdkafkaVersion);

var producer = new Kafka.Producer({
    'api.version.request': 'true',
    'bootstrap.servers': config['bootstrap_servers'],
    'dr_cb': true,
    'dr_msg_cb': true,
    'security.protocol' : 'SASL_PLAINTEXT',
    'sasl.mechanisms' : 'PLAIN',
    'sasl.username' : config['sasl_plain_username'],
    'sasl.password' : config['sasl_plain_password']
});

var connected = false

producer.setPollInterval(100);

producer.connect();

producer.on('ready', function() {
  connected = true
  console.log("connect ok")

  });

function produce() {
  try {
    producer.produce(
      config['topic_name'],
      new Buffer('Hello CKafka SASL'),
      null,
      Date.now()
    );
  } catch (err) {
    console.error('Error occurred when sending message(s)');
    console.error(err);
  }
}

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

producer.on('delivery-report', function(err, report) {
    console.log("delivery-report: producer ok");
});
// Any errors we encounter, including connection errors
producer.on('event.error', function(err) {
    console.error('event.error:' + err);
})

setInterval(produce,1000,"Interval");

