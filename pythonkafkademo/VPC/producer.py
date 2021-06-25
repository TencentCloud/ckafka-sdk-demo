#coding:utf8
from kafka import KafkaProducer

producer = KafkaProducer(
    bootstrap_servers = ['$domainName:$port'],
    api_version = (0, 10, 0))

message = "Hello World! Hello Ckafka!"
msg = json.dumps(message).encode()
producer.send('topic_name',value = msg)
print("produce message " + message + " success.");
producer.close()
