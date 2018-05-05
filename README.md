# reactive-vertx-frontend

The aim of the repository is to describe an asynchronous solution from the UI to the BackEnd :
- Vertx for the microservice (service subscriber and manual service producer)
- Kafka to manage stream (this is no the subject it is treated briefly)
- A simple Frontend in HTML with SocksJs websocket subscription


## Part One - Manage Kafka Service

-How to start kafka & Zookeeper (simple method)

Deploy Zookeeper and Kafka binaries into there extraction directory
Modify CFG file of both

start cmd for windows a batch file

    @echo off
    echo "start Zookeeper"
    start zkServer
    echo "Kafka"
    cd "path\kafka\kafka_2.11-1.1.0\bin\windows" 
    Start kafka-server-start.bat path\kafka\kafka_2.11-1.1.0\config\server.properties


Or unix style

    #!/bin/bash
    # Script to start Kafka instance
    bin/zookeeper-server-start.sh config/zookeeper.properties
    bin/kafka-server-start.sh config/server.properties
    

Or docker style :) (very efficient)

    docker run -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=`docker-machine ip \`docker-machine active\`` --env ADVERTISED_PORT=9092 spotify/kafka

For more info please follow the Dzone Guide to start the cluster (Reference [3])


## Part Two - Write simple UI to subscribe to a channel

The simple UI is a simple HTML file as describe below

    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <title>The asynchronous actions!</title>

        <script src="//cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js"></script>
        <script src="js/vertx-eventbus.js"></script>
        <script src="js/realtime-actions.js"></script>
    </head>
        <body>
        <h3>action</h3>
        <div id="error_message"></div>
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



## Part Three - Write a Producer and Consumer Vertx Verticles




## Part Four - Call test service (with postman)





# FAQ

## Websocket API vs SockJS

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
