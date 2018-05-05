package org.acamu.vertx.rest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

//https://vertx.io/blog/real-time-bidding-with-websockets-and-vert-x/
public class UserInterfaceServiceVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInterfaceServiceVerticle.class);

    public String current;

    @Override
    public void start() {
        LOGGER.info("Start of My Verticle");

// add cors allow to localhost:8181 address
        Router router = Router.router(vertx);

        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("accept");
        allowedHeaders.add("X-PINGARUNER");

        Set<HttpMethod> allowedMethods = new HashSet<>();
      //  allowedMethods.add(HttpMethod.GET);
        allowedMethods.add(HttpMethod.POST);
      //  allowedMethods.add(HttpMethod.DELETE);
     //   allowedMethods.add(HttpMethod.PATCH);
     //   allowedMethods.add(HttpMethod.OPTIONS);
      //  allowedMethods.add(HttpMethod.PUT);

        // * or other like "http://localhost:8080"
        router.route().handler(io.vertx.ext.web.handler.CorsHandler.create("*")
                .allowedHeaders(allowedHeaders)
                .allowedMethods(allowedMethods));
        //.allowCredentials(true));

        router.route("/eventbus/*").handler(eventBusHandler());

        //router.mountSubRouter("/api", apiRouter());
        router.route().failureHandler(errorHandler());
        router.route().handler(staticHandler());

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }

    private SockJSHandler eventBusHandler() {
        BridgeOptions options = new BridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddressRegex("correlationId\\.[0-9]+"));
        return SockJSHandler.create(vertx).bridge(options, event -> {
            if (event.type() == BridgeEventType.SOCKET_CREATED) {
                LOGGER.info("A socket was created");
            }
            event.complete(true);
        });
    }

    private Router apiRouter() {

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.route().consumes("application/json");
        router.route().produces("application/json");

        router.route();
        router.post("/requestControll/:id").handler(this::handleControllPoint);

        return router;
    }

    private ErrorHandler errorHandler() {
        return ErrorHandler.create(true);
    }

    private StaticHandler staticHandler() {
        return StaticHandler.create()
                .setCachingEnabled(false);
    }

    public void handleControllPoint(RoutingContext routingContext) {

        LOGGER.info("Vertx:getControllPoints");

        String correlationId = routingContext.request().getParam("id");
      /*
        Optional<ControlPoint> auction = this.repository.getById(auctionId);

        if (auction.isPresent()) {
            LOGGER.info("Vertx:getControllPoints:OK");
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(Json.encodePrettily(auction.get()));
        } else {
            LOGGER.info("Vertx:getControllPoints:NOK");
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(404)
                    .end();
        }
        */
    }
}
