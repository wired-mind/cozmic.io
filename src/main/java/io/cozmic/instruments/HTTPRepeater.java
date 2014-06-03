/*
 *
 *  * Copyright (c) 2014, Wired-Mind Labs, LLC. All Rights Reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/owner_address/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.cozmic.instruments;

import io.cozmic.CozmicComponent;
import io.cozmic.CozmicMessage;
import io.cozmic.ObjectBytes;
import io.cozmic.minders.ResponseType;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by Eric on 4/22/2014.
 */
public class HTTPRepeater extends CozmicComponent implements Instrument {

    private final Map<String, HttpClient> targetClientMap = new HashMap<>();
    private SimpleLoadBalancer loadBalancer;
    private JsonArray targets;
    private boolean loadbalanced;
    public long requestTimeOut = 60000L;

    @Override
    public void start() {
        super.start();

        String registrationAddress = config.getString(("registration_address"));
        loadbalanced = config.getBoolean("load_balanced", false);
        targets = config.getArray("targets");

        createTargetClients(targets);

        if (loadbalanced) {
            loadBalancer = new SimpleLoadBalancer(targets);
        }

        @SuppressWarnings("unchecked")
        Handler<Message<JsonObject>> registrationMessageHandler = new RegistrationHandler();

        eb.registerHandler(registrationAddress, registrationMessageHandler);

        @SuppressWarnings("unchecked")
        Handler<Message<byte[]>> repeatRequestHandler = new RepeatRequestHandler();

        eb.registerHandler(inboundAddress, repeatRequestHandler);
    }

    public void onConfigUpdate() {
        createTargetClients(this.targets);

    }

    void createTargetClients(JsonArray targets) {

        for (Object target : targets) {
            String host = ((JsonObject) target).getString("host");
            int port = ((JsonObject) target).getInteger("port");

            addTargetClient(host, port);
        }

    }

    void addTargetClient(final String host, final int port) {

        int maxPoolSizeDefault = 10;
        HttpClient targetClient = vertx.createHttpClient()
                .setPort(port)
                .setHost(host)
                .setKeepAlive(false)
                .setMaxPoolSize(maxPoolSizeDefault);

        targetClientMap.put(host + ":" + port, targetClient);

    }

    void removeTargetClient(String host, int port) {

        String targetKey = host + ":" + port;

        if (targetClientMap.containsKey(targetKey)) {
            targetClientMap.remove(targetKey);
        }

    }


    public class SimpleLoadBalancer {

        private Map<String, Target> targetMap;
        private int activeTargetCount = 0;
        private LinkedList<String> orderList;
        public long retryWait = 30000;

        public SimpleLoadBalancer(JsonArray targets) {
            setTargets(targets);

        }

        public SimpleLoadBalancer(JsonArray targets, long retryWait) {

            this.retryWait = retryWait;
            setTargets(targets);

        }

        public void setTargets(JsonArray targets) {
            this.targetMap = new HashMap<>();
            this.orderList = new LinkedList<>();

            for (Object target : targets) {
                String host = ((JsonObject) target).getString("host");
                int port = ((JsonObject) target).getInteger("port");

                addTarget(host, port);
            }

        }

        public int getActiveTargetCount() {
            return this.activeTargetCount;
        }

        public Object[] nextTarget() {

            String targetKey = orderList.removeFirst();
            orderList.addLast(targetKey);

            Target target = targetMap.get(targetKey);

            while (!target.active) {

                if ((target.lastCalled.getTime() - target.getTimestamp().getTime()) < retryWait) {
                    targetKey = orderList.removeFirst();
                    orderList.addLast(targetKey);
                    target = targetMap.get(targetKey);

                } else {
                    target.active = true;
                }

            }

            Object[] nextTarget = {target.host, target.port, target.lastCalled};

            target.called();

            return nextTarget;

        }

        public void addTarget(String host, int port) {

            Target target = new Target(host, port);

            targetMap.put(host + ":" + port, target);
            orderList.push(host + ":" + port);
            setTargetActive(host + ":" + port);

        }

        public void removeTarget(String host, int port) {

            String targetKey = host + ":" + port;

            if (targetMap.containsKey(targetKey)) {
                targetMap.remove(targetKey);
                orderList.remove(targetKey);
                activeTargetCount--;
            }

        }


        public void setTargetActive(String targetName) {

            targetMap.get(targetName).active = true;
            activeTargetCount++;

        }

        public void setTargetInactive(String targetName) {

            if (getActiveTargetCount() > 1) {
                targetMap.get(targetName).active = false;
                activeTargetCount--;
            }


        }

        public boolean isActive(String targetName) {

            return targetMap.get(targetName).active;

        }

        private class Target {

            public final String host;
            public final int port;
            public boolean active;
            public Timestamp lastCalled;

            public Target(String host, int port) {

                this.host = host;
                this.port = port;
                this.active = true;

            }

            public void called() {

                this.lastCalled = getTimestamp();

            }

            private Timestamp getTimestamp() {

                java.util.Date date = new java.util.Date();
                return new Timestamp(date.getTime());

            }
        }

    }

    private class RegistrationHandler implements Handler<Message<JsonObject>> {
        public void handle(final Message<JsonObject> message) {
            String action = message.body().getString("action").toLowerCase();
            String host = message.body().getString("host").toLowerCase();
            int port = message.body().getInteger("port");
            boolean success = false;

            if (loadbalanced) {
                switch (action) {
                    case "register":
                        addTargetClient(host, port);
                        loadBalancer.addTarget(host, port);
                        success = true;
                        break;
                    case "unregister":
                        loadBalancer.removeTarget(host, port);
                        removeTargetClient(host, port);
                        success = true;
                        break;
                    default:
                        success = false;
                        break;
                }
                message.reply(success);
                logger.info("new server (un)registration - " + host + ":" + port + " succeeded");

            } else {
                message.reply(success);
                logger.error("No Load balancer, can't add target");
            }

        }

    }

    private class RepeatRequestHandler implements Handler<Message<byte[]>> {
        public void handle(final Message<byte[]> message) {
            final CozmicMessage cm = CozmicMessage.fromBytes(message.body());

            try {
                @SuppressWarnings("unchecked")
                Map<String, ?> requestObjectMap = (Map<String, ?>) ObjectBytes.getObject(cm.data());

                byte[] requestBytes = (byte[]) requestObjectMap.get("request");
                byte[] requestBody = (byte[]) requestObjectMap.get("body");

                final JSONObject originalRequestObject = ObjectBytes.getJSONObject(requestBytes);
                final Buffer originalRequestBody = new Buffer(requestBody);


                JSONArray paramsArray = originalRequestObject.getJSONArray("params");

                URIBuilder uriBuilder = new URIBuilder();
                uriBuilder.setPath(originalRequestObject.getString("path"));


                for (int i = 0; i < paramsArray.length(); i++) {
                    String param = paramsArray.getString(i);
                    uriBuilder.setParameter(param.split("=")[0], param.split("=")[1]);
                }

                String originalPath = uriBuilder.build().toString();

                final String host;
                final int port;

                if (loadbalanced) {
                    Object[] target = loadBalancer.nextTarget();
                    host = target[0].toString();
                    port = (int) target[1];

                } else {
                    JsonObject target = targets.get(0);
                    host = target.getString("host");
                    port = target.getInteger("port");
                }


                HttpClient client = targetClientMap.get(host + ":" + port);

                client.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        logger.error(event.getMessage());
                        if (loadbalanced && loadBalancer.activeTargetCount > 1) {
                            loadBalancer.setTargetInactive(host + ":" + port);
                        }
                        message.fail(ResponseType.FailWithRetry.value(), "HttpClient timeout error");
                    }
                });


                HttpClientRequest repeatRequest = client.request(originalRequestObject.getString("method"), originalPath, new Handler<HttpClientResponse>() {
                    public void handle(HttpClientResponse response) {
                        logger.info("Received response of - " + response.statusCode() + " from " + host + ":" + port);
                        if (response.statusCode() == 200) {
                            message.reply(true);

                        } else if (response.statusCode() == 400) {
                            message.fail(ResponseType.FailNoRetry.value(), "Received response of - " + response.statusCode() + " from " + host + ":" + port);
                            logger.info("Malformed request, discarding message");

                        } else {
                            if (loadbalanced && loadBalancer.activeTargetCount > 1) {
                                loadBalancer.setTargetInactive(host + ":" + port);
                            }

                            message.fail(ResponseType.FailWithRetry.value(), "Received response of - " + response.statusCode() + " from " + host + ":" + port);
                            logger.info("failing message and setting retry status");
                        }
                    }
                });

                repeatRequest.exceptionHandler(new Handler<Throwable>() {
                    public void handle(Throwable t) {
                        message.fail(ResponseType.FailWithRetry.value(), "HTTP Request error");
                    }
                });

                JSONArray headersArray = originalRequestObject.getJSONArray("headers");

                for (int i = 0; i < headersArray.length(); i++) {
                    String header = headersArray.getString(i);
                    uriBuilder.setParameter(header.split("=")[0], header.split("=")[1]);
                }

                try {
                    repeatRequest.setTimeout(requestTimeOut);
                    repeatRequest.setChunked(true);
                    repeatRequest.write(originalRequestBody.getString(0, originalRequestBody.length(), "utf-8"));

                } catch (Exception ex) {
                    message.fail(ResponseType.FailWithRetry.value(), "Request timed out");


                } finally {
                    repeatRequest.end();
                    repeatRequest = null;
                }

            } catch (Exception ex) {
                logger.info("repeat failed - " + ex.getMessage());
                message.fail(ResponseType.FailWithRetry.value(), "Repeat failed");

            }

        }
    }
}
