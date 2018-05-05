package org.acamu.vertx.kafka;

import io.vertx.core.json.Json;
import org.acamu.vertx.domain.ControllPoint;
import org.apache.kafka.common.serialization.StringDeserializer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.kafka.client.consumer.KafkaReadStream;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;

//https://dzone.com/articles/running-apache-kafka-on-windows-os
//kafka-server-start.bat E:\Developement\2-tools-deployed\kafka\kafka_2.11-1.1.0\config\server.properties
//Start zk : zkserver
//Start Kafka : kafka-server-start.bat E:\Developement\2-tools-deployed\kafka\kafka_2.11-1.1.0\config\server.properties
//Send messge to consumer on windows
//kafka-console-producer.bat --broker-list localhost:9092 --topic websocket_bridge

//https://zeroturnaround.com/rebellabs/using-vert-x-to-connect-the-browser-to-a-message-queue/
public class KafkaConsumerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerVerticle.class);

   // private KafkaReadStream<String, String> consumer;

    @Override
    public void start(Future<Void> future) {

        LOGGER.info("Start Kafka consumer");

        final KafkaReadStream<String, String> consumer = createConsumer();

        // we are ready w/ deployment
        future.complete();
    }

    private KafkaReadStream<String, String> createConsumer() {
        LOGGER.info("Start Kafka consumer");

        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "mygroup2");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        KafkaReadStream<String, String> consumer;

        consumer = KafkaReadStream.create(vertx, config);

        consumer.subscribe(Collections.singleton("websocket_bridge"), ar -> {
            if (ar.succeeded()) {
                LOGGER.info("Subscribed");
            } else {
                LOGGER.error("Could not subscribe: err={}", ar.cause().getMessage());
            }
        });

        consumer.handler(record -> {
            System.out.println("Processing key=" + record.key() + ",value=" + record.value() +
                    ",partition=" + record.partition() + ",offset=" + record.offset());

            ControllPoint controllPoint = Json.decodeValue(record.value(), ControllPoint.class);
            LOGGER.info("ControllPoint processed: id={}, price={}", controllPoint.getId(), controllPoint.getPrice());
            String jsonEncode = Json.encode(controllPoint);
            LOGGER.info("send =>:"+jsonEncode);
            vertx.eventBus().publish("correlationId." + controllPoint.getId(), jsonEncode);

            //TODO Save to Memory event to ulterior use or something else
        });

        return consumer;
    }
}