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
    p, err := kafka.NewProducer(&kafka.ConfigMap{
        // 设置接入点，请通过控制台获取对应Topic的接入点。
        "bootstrap.servers": strings.Join(cfg.Servers, ","),
        // 用户不显示配置时，默认值为1。用户根据自己的业务情况进行设置
        "acks": 1,
        // 请求发生错误时重试次数，建议将该值设置为大于0，失败重试最大程度保证消息不丢失
        "retries": 0,
        // 发送请求失败时到下一次重试请求之间的时间
        "retry.backoff.ms": 100,
        // producer 网络请求的超时时间。
        "socket.timeout.ms": 6000,
        // 设置客户端内部重试间隔。
        "reconnect.backoff.max.ms": 3000,
    })
    if err != nil {
        log.Fatal(err)
    }
    defer p.Close()
    // 产生的消息 传递至报告处理程序
    go func() {
        for e := range p.Events() {
            switch ev := e.(type) {
            case *kafka.Message:
                if ev.TopicPartition.Error != nil {
                    fmt.Printf("Delivery failed: %v\n", ev.TopicPartition)
                } else {
                    fmt.Printf("Delivered message to %v\n", ev.TopicPartition)
                }  
            }
        }
    }()
    // 异步发送消息
    topic := cfg.Topic[0]
    for _, word := range []string{"Confluent-Kafka", "Golang Client Message"} {
        _ = p.Produce(&kafka.Message{
            TopicPartition: kafka.TopicPartition{Topic: &topic, Partition: kafka.PartitionAny},
            Value:          []byte(word),
        }, nil)
    }
    // 等待消息传递
    p.Flush(10 * 1000)
}
