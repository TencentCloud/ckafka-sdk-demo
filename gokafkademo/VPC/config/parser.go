package config

import (
	"encoding/json"
	"os"
)

// CKafkaConfig Ckafka配置项
type CKafkaConfig struct {
	Topic           []string   `json:"topic"`
	SASL            CKafkaSASL `json:"sasl"`
	Servers         []string   `json:"bootstrapServers"`
	ConsumerGroupId string     `json:"consumerGroupId"`
}

// CKafkaSASL sasl配置
type CKafkaSASL struct {
	Username   string `json:"username"`
	Password   string `json:"password"`
	InstanceId string `json:"instanceId"`
}

// ParseConfig 配置解析结构
func ParseConfig(configPath string) (*CKafkaConfig, error) {
	fileContent, err := os.Open(configPath)
	if err != nil {
		return nil, err
	}
	decoder := json.NewDecoder(fileContent)
	c := &CKafkaConfig{}
	decodeError := decoder.Decode(c)
	if decodeError != nil {
		return nil, decodeError
	}
	return c, nil
}
