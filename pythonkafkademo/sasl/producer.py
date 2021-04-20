#coding:utf8
from kafka import KafkaProducer

producer = KafkaProducer(
    bootstrap_servers = ['$ip:$port'],
    ssl_check_hostname = False,
    security_protocol = "SASL_PLAINTEXT",
    sasl_mechanism = "PLAIN",
    sasl_plain_username = "$instance_id#$username",
    sasl_plain_password = "$password",
    api_version = (0, 10, 0))

producer.send('$topic_name', value = "Hello World! Hello Ckafka!")
producer.close()
