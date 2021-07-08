package main

import (
	"fmt"
	"gokafkademo/config"
	"log"
	"strings"

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func main() {

	cfg, err := config.ParseConfig("../config/kafka.json")
	if err != nil {
		log.Fatal(err)
	}

	c, err := kafka.NewConsumer(&kafka.ConfigMap{
		// 设置接入点，请通过控制台获取对应Topic的接入点。
		"bootstrap.servers": strings.Join(cfg.Servers, ","),
		// SASL 验证机制类型默认选用 PLAIN
		"sasl.mechanism": "PLAIN",
		// 在本地配置 ACL 策略。
		"security.protocol": "SASL_PLAINTEXT",
		// username 是实例 ID + # + 配置的用户名，password 是配置的用户密码。
		"sasl.username": fmt.Sprintf("%s#%s", cfg.SASL.InstanceId, cfg.SASL.Username),
		"sasl.password": cfg.SASL.Password,
		// 设置的消息消费组
		"group.id":          cfg.ConsumerGroupId,
		"auto.offset.reset": "earliest",

		// 使用 Kafka 消费分组机制时，消费者超时时间。当 Broker 在该时间内没有收到消费者的心跳时，认为该消费者故障失败，Broker
		// 发起重新 Rebalance 过程。目前该值的配置必须在 Broker 配置group.min.session.timeout.ms=6000和group.max.session.timeout.ms=300000 之间
		"session.timeout.ms": 10000,
		
	})

	if err != nil {
		log.Fatal(err)
	}
	// 订阅的消息topic 列表
	err = c.SubscribeTopics([]string{"test", "test-topic"}, nil)
	if err != nil {
		log.Fatal(err)
	}

	for {
		msg, err := c.ReadMessage(-1)
		if err == nil {
			fmt.Printf("Message on %s: %s\n", msg.TopicPartition, string(msg.Value))
		} else {
			// 客户端将自动尝试恢复所有的 error
			fmt.Printf("Consumer error: %v (%v)\n", err, msg)
		}
	}

	c.Close()
}
