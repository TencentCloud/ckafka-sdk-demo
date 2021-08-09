#coding:utf8
from kafka import KafkaProducer
import json
producer = KafkaProducer(
    bootstrap_servers = ['xx.xx.xx.xx:port'],
    ssl_check_hostname = False,
    security_protocol = "SASL_PLAINTEXT",
    sasl_mechanism = "PLAIN",
    sasl_plain_username = "yourInstanceId#username",
    sasl_plain_password = "password",
    api_version = (0, 10, 0))
message = "Hello World! Hello Ckafka!"
msg = json.dumps(message).encode()
producer.send('yourTopicName',value = msg)
print("produce message " + message + " success.");
producer.close()
