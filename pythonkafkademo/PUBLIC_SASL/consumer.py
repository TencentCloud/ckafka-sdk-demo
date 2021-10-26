#coding:utf8
from kafka import KafkaConsumer

consumer = KafkaConsumer(
    'topic_name',
    group_id = "group_id",
    bootstrap_servers = ['xx.xx.xx.xx:port'],
    api_version = (1,1),

    #
    # SASL_PLAINTEXT 公网接入
    #
    security_protocol = "SASL_PLAINTEXT",
    sasl_mechanism = 'PLAIN',
    sasl_plain_username = "instanceId#username",
    sasl_plain_password = "password",

    #
    # SASL_SSL 公网接入
    #
    # security_protocol = "SASL_SSL",
    # sasl_mechanism = 'PLAIN',
    # sasl_plain_username = "instanceId#username",
    # sasl_plain_password = "password",
    # ssl_cafile = "CARoot.pem",
    # ssl_certfile = "certificate.pem",
    # ssl_check_hostname = False,

)


for message in consumer:
    print ("Topic:[%s] Partition:[%d] Offset:[%d] Value:[%s]" %
    (message.topic, message.partition, message.offset, message.value))
