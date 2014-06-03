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

package io.cozmic.channels.WebSocket;

import io.cozmic.ObjectBytes;
import io.cozmic.channels.Channel;
import io.cozmic.channels.HTTP.HTTPRequestSerializer;
import org.json.JSONObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.streams.Pump;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;


/**
 * Created by Eric on 4/29/2014.
 */
public class DeviceWSChannel extends Channel {
    private HttpServer httpServer;
    private String keyStore;
    private String keyStorePassword;
    public boolean useSSL;

    @Override
    public void start() {
        super.start();

        this.port = config.getInteger("port");
        this.host = config.getString("host");
        this.keyStore = config.getString("key_store");
        this.keyStorePassword = config.getString("key_store_password");
        this.useSSL = config.getBoolean("use_ssl");

        httpServer = vertx.createHttpServer()
                .setAcceptBacklog(10000);
        if (useSSL) {
            httpServer.setSSL(true)
                    .setKeyStorePath("/path/to/your/keystore/server-keystore.jks")
                    .setKeyStorePassword("password");
        }

        httpServer.websocketHandler(new Handler<ServerWebSocket>() {
            public void handle(final ServerWebSocket ws) {
                ws.dataHandler(new Handler<Buffer>() {
                    public void handle(Buffer data) {
                        try {
                            byte[] messageBytes = ObjectBytes.getBytes(data);
                            eb.send(conduitAddress, messageBytes);

                        } catch (Exception ex) {
                            logger.error(ex.getMessage());
                        }
                    }
                });
            }
        }).listen(port, host);


    }
}
