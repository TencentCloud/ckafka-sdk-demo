# coding:utf8
from kafka import KafkaProducer
import json

producer = KafkaProducer(
    bootstrap_servers = ['xx.xx.xx.xx:port'],
    api_version = (1, 1),

    #
    # SASL_PLAINTEXT 公网接入
    #
    security_protocol = "SASL_PLAINTEXT",
    sasl_mechanism = "PLAIN",
    sasl_plain_username = "instanceId#username",
    sasl_plain_password = "password",

    #
    # SASL_SSL 公网接入
    #
    # security_protocol = "SASL_SSL",
    # sasl_mechanism = "PLAIN",
    # sasl_plain_username = "instanceId#username",
    # sasl_plain_password = "password",
    # ssl_cafile = "CARoot.pem",
    # ssl_certfile = "certificate.pem",
    # ssl_check_hostname = False,

)

message = "Hello World! Hello Ckafka!"
msg = json.dumps(message).encode()
producer.send('topic_name', value = msg)
print("produce message " + message + " success.")
producer.close()
