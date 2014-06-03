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

package io.cozmic.channels.HTTP;

import io.cozmic.channels.Channel;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;


/**
 * Created by Eric on 5/25/2014.
 */
public abstract class AbstractHTTPChannel extends Channel {
    protected HttpServer httpServer;
    private String keyStore;
    private String keyStorePassword;
    public boolean useSSL;

    @Override
    public void start() {
        super.start();

        this.port = config.getInteger("port");
        this.host = config.getString("host");
        this.keyStore = config.getString("key_store", "");
        this.keyStorePassword = config.getString("key_store_password","");
        this.useSSL = config.getBoolean("use_ssl", false);

        httpServer = vertx.createHttpServer()
                .setAcceptBacklog(10000);
        if (useSSL) {
            httpServer.setSSL(true)
                    .setKeyStorePath(keyStore)
                    .setKeyStorePassword(keyStorePassword);
        }



    }

    protected class HttpServerRequestHandler implements Handler<HttpServerRequest> {
        public void handle(final HttpServerRequest request) {


        }

    }

    protected abstract class BodyHandler implements Handler<Buffer> {
        private final HttpServerRequest request;

        BodyHandler(HttpServerRequest request) {
            this.request = request;
        }

        public abstract void handle(Buffer body);
    }

}

