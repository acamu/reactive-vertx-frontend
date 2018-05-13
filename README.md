---
title: Real-time manage actions with Websockets and Vert.x part 2 - Kafka
template: post.html
date: 2018-05-05
author: acamu
---

A simple sample of an full application waiting for injected data into Kafka cluster. This give bullets to built an event application with vertx backEnd services and a simple FrontEnd (which can be easyly replaced by an Angular or something else).


## Real-time manage actions with Websockets and Vert.x part 2 - Kafka - FrontEnd-BackEnd

The aim of this post is to describe an asynchronous solution from the UI to the BackEnd with the use of apache Kafka. We are going to show how to subscribe to a channel, order somes actions and wait response. This is the continuation of the post [`Real-time bidding with Websockets and Vert.x`](https://vertx.io/blog/real-time-bidding-with-websockets-and-vert-x/) and pushes further.

The project is organize as following:

- Main Vertx service (Main launcher)
- Vertx kafka subscriber & producer to manage stream (this is no the subject it is treated briefly)
- A simple Frontend in HTML with SocksJs websocket subscription which is a correlationID (to subscribe to a specific channel)

## Server part
### Part One - Data bus - Kafka/Zookeeper 

You have to start kafka/Zookeeper which can be already existing in the SI (Manage event flow).
There is few method to deploy the stack i will explain most part of them.

#### Deploy binaries on windows
Deploy Zookeeper and Kafka binaries into there extraction directory
Modify CFG file of both to use the correct port and directory (e.g: log directory)

    Launch a Windows batch file

    @echo off
    echo "Start Zookeeper"
    start zkServer
    
    echo "Start Kafka"
    cd "path\kafka\kafka_2.11-1.1.0\bin\windows" 
    Start kafka-server-start.bat path\kafka\kafka_2.11-1.1.0\config\server.properties

#### Deploy binaries on Unix
    Launch a Unix sh file

    #!/bin/bash
    # Script to start Kafka instance
    bin/zookeeper-server-start.sh config/zookeeper.properties
    bin/kafka-server-start.sh config/server.properties
    

#### Deploy on docker (very efficient : easy to start,  no trace on your computer, etc...)

    docker run -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=`docker-machine ip \`docker-machine active\`` --env ADVERTISED_PORT=9092 spotify/kafka
    
    or
    
    ifconfig and check docker host ip
    docker run -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=your-host-ip --env ADVERTISED_PORT=9092 spotify/kafka
    
    
    if you stop and after start :
    
    docker exec -it yourimagesId bash
    the scripts are under this directory : /opt/kafka_2.11-0.10.1.0/bin

Some usefull commands

    Create a topics : ./kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic sample-topic
    List all existing topics : ./kafka-topics.sh --list --zookeeper localhost:2181
    Send messages : ./kafka-console-producer.sh --broker-list localhost:9092 --topic sample-topic
    Start Consumer (your application will be the consumer!) : ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic sample-topic --from-beginning

For more info please follow the Dzone Guide to start the cluster (Reference [3]) on windows


### Part Two - FrontEnd to subscribe to a specific channel

The simple webpage contains one javascript file which subscribe and update the channel with incomming messages.
HTML source code like this : 

    <html>
    <head>
    .....
        <script src="js/vertx-eventbus.js"></script>
        <script src="js/realtime-actions.js"></script>
    </head>
        <body>
       ....
        <form>
            Current Correlation_id:
            <span id="current_correlation_id"></span>
            <br/>
            Current content:
            <span id="current_content"></span>
            <br/>
            <div>
                <label for="correlation_id">Your current correlation_id:</label>
                <input id="correlation_id" type="text">
                <input type="button" onclick="registerHandlerForUpdateFeed();" value="Subscribe">
            </div>
            <div>
                Feed:
                <textarea id="feed" rows="4" cols="50" readonly></textarea>
            </div>
        </form>
        </body>
    </html>

I use the `vertx-eventbus.js` library to create a connection to the event bus. `vertx-eventbus.js` library is a part of the Vert.x distribution. And a specific JS file subscribe to a channel `realtime-actions.js`.
The user can subscribe as much as channel he wants. It will be notified when a new flow are incomming (the feed field will be updated be the handler).

Below the code snippet of the `realtime-actions.js`
    
    function registerHandlerForUpdateFeed() {
        var correlation_id = document.getElementById('correlation_id').value;
        console.log('=>' + correlation_id);
        document.getElementById('current_correlation_id').innerHTML = correlation_id;
        var eventBus = new EventBus('http://localhost:8080/eventbus');
        eventBus.onopen = function () {
            eventBus.registerHandler('correlationId.' + correlation_id, function (error, message) {
                //console.log(message.body);
                var obj = JSON.parse(message.body);
                var s = JSON.stringify(message.body)
                document.getElementById('current_content').innerHTML = obj;
                document.getElementById('feed').value += 'New content: ' + s + '\n';
            });
        }
    };


### Part Three - Kafka Producer and Consumer Verticles

There is a lot a things to discuss you can in first going to the doc page "https://vertx.io/docs/vertx-kafka-client/java/"
I will not go in the detail but we need to inherit as usual from [`AbstractVerticle`](http://vertx.io/docs/apidocs/io/vertx/core/AbstractVerticle.html) and override the start method.
In my case i created a private method **createConsumer** which has to deal with the creation of the connection stream. I use the default Vertx **StringDeserializer** and subscribe to a topic named  **websocket_bridge**.

The consumer handler will read the stream on the topic and decode the flow to read value. It will propagate to the true channel by use of the correlationId provided in the object **"correlationId." + currentId**.

Below the code snippet of the `KafkaConsumerVerticle.java`

#### Kafka service Consumer

    public class KafkaConsumerVerticle extends AbstractVerticle {

        private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerVerticle.class);

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

                // TODO you can do some stuff here
                ControllPoint controllPoint = Json.decodeValue(record.value(), ControllPoint.class);
                LOGGER.info("ControllPoint processed: id={}, price={}", controllPoint.getId(), controllPoint.getPrice());
              
                vertx.eventBus().publish("correlationId." + controllPoint.getId(), record.value());
            });
            return consumer;
        }
    }

Below the code snippet of the `KafkaProducerVerticle.java`

#### Kafka service Producer (for the need of the sample)

Another Verticle Class we need to inherit as usual from [`AbstractVerticle`](http://vertx.io/docs/apidocs/io/vertx/core/AbstractVerticle.html) and override the start method. Same punishment i wrote a private method to connect to Kafka server **KafkaProducer**.

I need a route to manage incomming call which request a new object creation in the kafka cluster the route is defineD as follow `http://localhost:8090/controllpoint` and use the method **POST**. The object must look like that `{"id":4,"content":"test content","validated":false,"price":134}`  

    public class KafkaProducerVerticle extends AbstractVerticle {

        private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducerVerticle.class);

        @Override
        public void start(Future<Void> future) {

            LOGGER.info("Start Kafka producer");
            final KafkaProducer<String, JsonObject> producer = createProducer();
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

#### Manipulated object - domain 

    public class ControllPoint implements Serializable {

        private long id;
        private String content;
        private boolean validated;
        private BigDecimal price;

        @JsonCreator
        public ControllPoint(@JsonProperty(value = "id", required = true) long id,
                             @JsonProperty(value = "content", required = true) String content,
                             @JsonProperty(value = "validated", required = true) boolean validated,
                             @JsonProperty(value = "price", required = true) BigDecimal price) {
            this.id = id;
            this.content = content;
            this.validated = validated;
            this.price = price;
        }

        public ControllPoint(long id, BigDecimal price) {
            this.id = id;
            this.price = price;
        }

        public ControllPoint(long id) {
            this(id, BigDecimal.ZERO);
        }

    ...Getter/Setter

        @Override
        public String toString() {
            return "{" +
                    "\"" + "id:" + "\"" + id + "\"" +
                    ", " + "\"" + "price:" + price + '\"' +
                    ", " + "\"" + "validated:" + validated + '\"' +
                    ", " + "\"" + "content:" + content + '\"' +
                    "}";
        }
    }

### Part Four MainVerticle class

First we need to inherit from [`AbstractVerticle`](http://vertx.io/docs/apidocs/io/vertx/core/AbstractVerticle.html) and override the start method. The start method will use a protected method **deployVerticle** which has to start verticle and ensure the child Verticle has been started.

    public class MainVerticle extends AbstractVerticle {
    
        private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

        @Override
        public void start() {
            LOGGER.info("Start of My Main Verticle");

            final Vertx vertx = Vertx.vertx();
            deployVerticle(KafkaConsumerVerticle.class.getName());
            deployVerticle(KafkaProducerVerticle.class.getName());
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


## Part Six - Call test service (with postman or something like restClient)

Full application code is available on [github](https://github.com/acamu/reactive-vertx-frontend).

    EndPoint : http://localhost:8090/controllpoint
    Method : POST
    Body : {"id" : 14, "content" : "test content", "validated"  :false, "price" : 134}


![Real time bidding in application](/assets/blog/cccccc/myimage.png "Real time bidding in application")


# ==========================================================
# FAQ

## Websocket API vs SockJS (extract ref [1])

Unfortunately, WebSockets are not supported by all web browsers. However, there are libraries that provide a fallback when WebSockets are not available. One such library is **SockJS.** SockJS starts from trying to use the WebSocket protocol. However, if this is not possible, it uses a variety of browser-specific transport protocols. SockJS is a library designed to work in all modern browsers and in environments that do not support WebSocket protocol, for instance behind restrictive corporate proxy. SockJS provides an API similar to the standard WebSocket API.

## Enable CORS:

    Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("accept");
        allowedHeaders.add("X-PINGARUNER");

        Set<HttpMethod> allowedMethods = new HashSet<>();
        allowedMethods.add(HttpMethod.GET);
        allowedMethods.add(HttpMethod.POST);
        allowedMethods.add(HttpMethod.DELETE);
        allowedMethods.add(HttpMethod.PATCH);
        allowedMethods.add(HttpMethod.OPTIONS);
        allowedMethods.add(HttpMethod.PUT);

        // * or other like "http://localhost:8080"
        router.route().handler(io.vertx.ext.web.handler.CorsHandler.create("*")
                .allowedHeaders(allowedHeaders)
                .allowedMethods(allowedMethods));


## How to subscribe to a specific channel



# References

[1] : https://vertx.io/blog/real-time-bidding-with-websockets-and-vert-x/

[2] : https://medium.com/oril/spring-boot-websockets-angular-5-f2f4b1c14cee

[3] : https://dzone.com/articles/running-apache-kafka-on-windows-os

[4] : https://kafka.apache.org/quickstart

[5] : https://hub.docker.com/r/spotify/kafka/
