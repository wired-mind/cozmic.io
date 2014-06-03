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

package io.cozmic.channels.TCP;


import io.cozmic.channels.Channel;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;


/**
 * Created by Eric on 4/29/2014.
 */
public class DeviceTCPChannel extends Channel {
    private NetServer netServer;
    private String keyStore;
    private String keyStorePassword;
    public boolean useSSL;

    @Override
    public void start() {
        super.start();

        this.port = config.getInteger("port");
        this.host = config.getString("host");
        this.keyStore = config.getString("key_store", "");
        this.keyStorePassword = config.getString("key_store_password", "");
        this.useSSL = config.getBoolean("use_ssl", false);

        netServer = vertx.createNetServer()
                .setAcceptBacklog(10000);
        if (useSSL) {
            netServer.setSSL(true)
                    .setKeyStorePath("/path/to/your/keystore/server-keystore.jks")
                    .setKeyStorePassword("password");
        }

        netServer.connectHandler(new Handler<NetSocket>() {
            public void handle(final NetSocket sock) {
                sock.dataHandler(new Handler<Buffer>() {
                    public void handle(Buffer buffer) {
                        sock.write(buffer);
                    }
                });
            }
        }).listen(port, host);

    }

}
