#coding:utf8
from kafka import KafkaProducer

producer = KafkaProducer(
    bootstrap_servers = ['$ip:$port'],
    api_version = (0, 10, 0))

producer.send('$topic', value = "Hello World! Hello Ckafka!")
producer.close()
