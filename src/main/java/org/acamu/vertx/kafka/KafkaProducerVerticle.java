package org.acamu.vertx.kafka;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;
import io.vertx.kafka.client.serialization.JsonObjectSerializer;
import org.acamu.vertx.domain.ControllPoint;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Properties;

//sample producer : {"id":4,"content":"test content","validated":false,"price":134}
//url to call http://localhost:8090/controllpoint
public class KafkaProducerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducerVerticle.class);

    //  private KafkaProducer<String, JsonObject> producer;

    @Override
    public void start(Future<Void> future) {

        LOGGER.info("Start Kafka producer");
        final KafkaProducer<String, JsonObject> producer = createProducer();
/*
        producer.partitionsFor("websocket_bridge", done -> {
            done.result().forEach(p -> LOGGER.info("Partition: id={}, topic={}", p.getPartition(), p.getTopic()));
        });
*/

        /*
        Create a route to call the sample kafka bean producer
        And specify the handler which accept call. In this sample only the post method is expected
         */
        Router router = Router.router(vertx);
        router.route("/controllpoint/*").handler(ResponseContentTypeHandler.create());
        router.route(HttpMethod.POST, "/controllpoint").handler(BodyHandler.create());
        router.post("/controllpoint").produces("application/json").handler(rc -> {

            //Receive body sample to create specialised bean with specific data
            LOGGER.info("body received =>"+rc.getBodyAsString());
            ControllPoint o = Json.decodeValue(rc.getBodyAsString(), ControllPoint.class);
            KafkaProducerRecord<String, JsonObject> record = KafkaProducerRecord.create("websocket_bridge", null, rc.getBodyAsJson(), 0);
            producer.write(record, done -> {
                if (done.succeeded()) {
                    RecordMetadata recordMetadata = done.result();
                    LOGGER.info("Record sent: msg={}, destination={}, partition={}, offset={}", record.value(), recordMetadata.getTopic(), recordMetadata.getPartition(), recordMetadata.getOffset());
                    o.setId(recordMetadata.getOffset());
                    o.setContent("PROCESSING");
                } else {
                    Throwable t = done.cause();
                    LOGGER.error("Error sent to topic: {}", t.getMessage());
                    o.setContent("REJECTED");
                }
                rc.response().end(Json.encodePrettily(o));
            });
        });
        vertx.createHttpServer().requestHandler(router::accept).listen(8090);

        // we are ready w/ deployment
        future.complete();
    }

    private KafkaProducer<String, JsonObject> createProducer() {
        LOGGER.info("Start Kafka consumer");

        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonObjectSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "1");
        KafkaProducer producer = KafkaProducer.create(vertx, config);

        return producer;
    }
}