package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class MainVerticle extends AbstractVerticle {

    private static final String MONGO_HOST = "localhost";
    private static final int MONGO_PORT = 27017;
    private static final String DB_NAME = "stationery_db";

    private MongoClient mongoClient;

    @Override
    public void start(Promise<Void> promise) {

        mongoClient = MongoClient.create(vertx, new JsonObject()
                .put("connection_string", "mongodb://" + MONGO_HOST + ":" + MONGO_PORT)
                .put("db_name", DB_NAME));

        //22BCAC19_BEDRE VAISHNAVI P
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/items").handler(this::addItem);
        router.get("/items").handler(this::getAllItems);
        router.post("/items/:itemId").handler(this::updateItem);
        router.post("/items/:itemId/delete").handler(this::deleteItem);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, ar -> {
                    if (ar.succeeded()) {
                        System.out.println("Server started on port 8080");
                        promise.complete();
                    } else {
                        System.out.println("Failed to start server");
                        promise.fail(ar.cause());
                    }
                });
    }

    private void addItem(RoutingContext ctx) {
        JsonObject newItemJson = ctx.getBodyAsJson();
        mongoClient.save("items", newItemJson, ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .setStatusCode(201)
                        .end();
            } else {
                ctx.response()
                        .setStatusCode(500)
                        .end(ar.cause().getMessage());
            }
        });
    }

    private void getAllItems(RoutingContext ctx) {
        mongoClient.find("items", new JsonObject(), res -> {
            if (res.succeeded()) {
                List<JsonObject> items = res.result();
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .end(Json.encodePrettily(items));
            } else {
                ctx.response()
                        .setStatusCode(500)
                        .end(res.cause().getMessage());
            }
        });
    }

    private void updateItem(RoutingContext ctx) {
        String itemId = ctx.pathParam("itemId");
        JsonObject updateItemJson = ctx.getBodyAsJson();
        JsonObject query = new JsonObject().put("itemId", itemId);
        JsonObject update = new JsonObject().put("$set", updateItemJson);
        mongoClient.updateCollection("items", query, update, ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .setStatusCode(200)
                        .end();
            } else {
                ctx.response()
                        .setStatusCode(500)
                        .end(ar.cause().getMessage());
            }
        });
    }

    private void deleteItem(RoutingContext ctx) {
        String itemId = ctx.pathParam("itemId");
        JsonObject query = new JsonObject().put("itemId", itemId);
        mongoClient.removeDocument("items", query, ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .setStatusCode(204)
                        .end();
            } else {
                ctx.response()
                        .setStatusCode(500)
                        .end(ar.cause().getMessage());
            }
        });
    }

    @Override
    public void stop() {
        mongoClient.close();
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MainVerticle());
    }
}
//22BCAC19