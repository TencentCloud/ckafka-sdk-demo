# 默认接入点收发消息

本文介绍Python客户端如何在VPC环境下通过SASL接入点接入消息队列CKafka版并使用PLAIN机制收发消息。


#### 前提条件
- [安装 python](https://www.python.org/downloads/)
- [安装 pip](https://pip-cn.readthedocs.io/en/latest/installing.html)

#### 添加 Python 依赖库
```bash
pip install kafka-python
```
#### 修改配置
##### 生产
```python
#coding:utf8
from kafka import KafkaProducer

producer = KafkaProducer(
	bootstrap_servers = ['$ip:$port'],
   	ssl_check_hostname = False,
   	security_protocol = "SASL_PLAINTEXT",
   	sasl_mechanism = "PLAIN",
	sasl_plain_username = "$instance_id#$username",
   	sasl_plain_password = "$password",
	api_version = (0,10,0)
)

producer.send('$topic_name',value="Hello World! Hello Ckafka!")
producer.close()
```
| 参数 | 描述 |
| --- | --- |
| bootstrap_servers | SASL接入点。在【消息队列 CKafka】控制台 -【xx实例详情页面】-【基本信息】-【接入方式】获取 |
| sasl_plain_username | 用户名，格式为 实例Id#用户名。在【消息队列 CKafka】控制台 -【用户管理】创建|
| sasl_plain_password | 密码。在【消息队列 CKafka】控制台 -【用户管理】创建|
| topic_name | Topic名称。在【消息队列 CKafka】控制台 -【xx实例详情页面】-【topic管理】创建和获取 |

##### 消费
```python
#coding:utf8
from kafka import KafkaConsumer

consumer = KafkaConsumer(
	'$topic_name',
	group_id = "$group_id",
	bootstrap_servers = ['$ip:$port'],
	security_protocol = "SASL_PLAINTEXT",
	sasl_mechanism = 'PLAIN',
	sasl_plain_username = "$instance_id#$username",
   	sasl_plain_password = "$password",
	api_version = (0,10,0)
)

for message in consumer:
    print ("Topic:[%s] Partition:[%d] Offset:[%d] Value:[%s]" % (message.topic, message.partition, message.offset, message.value))
```
| 参数 | 描述 |
| --- | --- |
| bootstrap_servers | SASL接入点。在【消息队列 CKafka】控制台 -【xx实例详情页面】-【基本信息】-【接入方式】或 【内网IP与端口】获取 |
| group_id | 消费者的组 Id，根据业务需求自定义 |
| sasl_plain_username | 用户名。格式为 实例Id#用户名，在【消息队列 CKafka】控制台 -【用户管理】创建|
| sasl_plain_password | 密码。在【消息队列 CKafka】控制台 -【用户管理】创建|
| topic_name | Topic名称。在【消息队列 CKafka】控制台 -【xx实例详情页面】-【topic管理】创建和获取 |
