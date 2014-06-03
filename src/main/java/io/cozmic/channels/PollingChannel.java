/*
 *
 *  * Copyright (c) 2014, Wired-Mind Labs, LLC. All Rights Reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/license/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.cozmic.channels;

import io.cozmic.CozmicComponent;
import io.cozmic.conduits.EmanatorQueueConduit;

import org.vertx.java.core.*;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Eric on 5/22/2014.
 */

public abstract class PollingChannel extends CozmicComponent implements IPollingChannel {
    protected String host;
    protected int port;
    public String conduitAddress;
    public String conduitOwnerName;
    public JsonArray endpoints;
    public String registrationAddress;
    private final Map<String, Object> endpointClientMap = new HashMap<>();
    public final int maxPoolSizeDefault = 10;


    @Override
    public void start() {
        super.start();
        try {
            conduitAddress = config.getString("conduit_address");
            conduitOwnerName = config.getString("name");
            JsonObject conduitConfig = config.getObject("emanator_conduit_config");

            container.deployVerticle(EmanatorQueueConduit.class.getName(), conduitConfig, 1, new AsyncResultHandler<String>() {
                public void handle(AsyncResult<String> deployResult) {
                    if (deployResult.succeeded()) {
                        String deploymentId = deployResult.result();
                        Registrar.put(conduitOwnerName + ".emanator.conduit", deploymentId);

                    } else {
                        deployResult.cause().printStackTrace();
                    }
                }
            });

        } catch (Exception ex) {

            logger.error(ex.getMessage());
            throw ex;

        }
        registrationAddress = config.getString(("registration_address"));
        endpoints = config.getArray("endpoints");

        createEndpointClients(endpoints);


        @SuppressWarnings("unchecked")
        Handler<Message<JsonObject>> registrationMessageHandler = new Handler<Message<JsonObject>>() {
            public void handle(final Message<JsonObject> message) {
                String action = message.body().getString("action").toLowerCase();
                String host = message.body().getString("host").toLowerCase();
                int port = message.body().getInteger("port");
                String protocol = message.body().getString("protocol").toLowerCase();
                boolean success = false;

                if (action.equals("register")) {
                    addEndpointClient(host, port, protocol);
                    success = true;
                } else if (action.equals("unregister")) {
                    removeEndpointClient(host, port, protocol);
                    success = true;
                } else {
                    success = false;
                }
                message.reply(success);
                logger.info("new server (un)registration - " + host + ":" + port + " succeeded");
            }
        };

        eb.registerHandler(registrationAddress, registrationMessageHandler);
    }

    public void onConfigUpdate() {
        createEndpointClients(this.endpoints);

    }

    public void createEndpointClients(JsonArray endpoints) {

        for (Object endpoint : endpoints) {
            String host = ((JsonObject) endpoint).getString("host");
            int port = ((JsonObject) endpoint).getInteger("port");
            String protocol = ((JsonObject) endpoint).getString("protocol");

            addEndpointClient(host, port, protocol);
        }

    }

    public void addEndpointClient(final String host, final int port, final String protocol) {


    }

    public void removeEndpointClient(final String host, final int port, final String protocol) {

        String targetKey = protocol + ":" + host + ":" + port;

        if (endpointClientMap.containsKey(targetKey)) {
            endpointClientMap.remove(targetKey);
        }

    }

}