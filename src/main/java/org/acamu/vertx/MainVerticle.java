package org.acamu.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.acamu.vertx.kafka.KafkaConsumerVerticle;
import org.acamu.vertx.kafka.KafkaProducerVerticle;
import org.acamu.vertx.rest.UserInterfaceServiceVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start() {
        LOGGER.info("Start of My Main Verticle");

        final Vertx vertx = Vertx.vertx();
        deployVerticle(KafkaConsumerVerticle.class.getName());
        deployVerticle(KafkaProducerVerticle.class.getName());

        deployVerticle(UserInterfaceServiceVerticle.class.getName());
    }

    protected void deployVerticle(String className) {
        vertx.deployVerticle(className, res -> {
            if (res.succeeded()) {
                System.out.printf("Deployed %s verticle \n", className);
            } else {
                System.out.printf("Error deploying %s verticle:%s \n", className, res.cause());
            }
        });
    }
}
