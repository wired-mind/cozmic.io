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
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.datagram.InternetProtocolFamily;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetServer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Eric on 5/24/2014.
 */
public abstract class ConfigurableChannel extends CozmicComponent implements IChannel {
    private Object channelServer;
    private Class channelClass;
    public String channelType;
    public String host;
    public int port;
    public int maxPoolSizeDefault;
    public String conduitAddress;
    public String conduitOwnerName;
    private String keyStore;
    private String keyStorePassword;
    public boolean useSSL;


    private boolean configureChannel() {

        switch (channelType) {
            case "HTTP":

                HttpServer httpServer = vertx.createHttpServer()
                        .setAcceptBacklog(10000);
                if (useSSL) {
                    httpServer.setSSL(true)
                            .setKeyStorePath(keyStore)
                            .setKeyStorePassword(keyStorePassword);
                }
                httpServer.requestHandler(new Handler<HttpServerRequest>() {
                    public void handle(final HttpServerRequest request) {

                    }
                });

                channelClass = HttpServer.class;
                channelServer = httpServer;
                break;

            case "WebSocket":

                HttpServer wsServer = vertx.createHttpServer()
                        .setAcceptBacklog(10000);
                if (useSSL) {
                    wsServer.setSSL(true)
                            .setKeyStorePath(keyStore)
                            .setKeyStorePassword(keyStorePassword);
                }

                channelClass = HttpServer.class;
                channelServer = wsServer;
                break;

            case "TCP":

                final NetServer netServer = vertx.createNetServer();
                channelClass = NetServer.class;
                channelServer = netServer;
                break;


            case "UDP":

                final DatagramSocket udpServer = vertx.createDatagramSocket(InternetProtocolFamily.IPv4);
                channelClass = DatagramSocket.class;
                channelServer = udpServer;
                break;

        }

        return true;
    }

    public void handleRequest() {

    }


    @Override
    public void start() {
        super.start();
        this.channelType = config.getString("channel_type");
        this.port = config.getInteger("port");
        this.host = config.getString("host");
        this.maxPoolSizeDefault = 10;
        this.keyStore = config.getString("key_store", "");
        this.keyStorePassword = config.getString("key_store_password", "");
        this.useSSL = config.getBoolean("use_ssl", false);

        configureChannel();

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
    }

}
