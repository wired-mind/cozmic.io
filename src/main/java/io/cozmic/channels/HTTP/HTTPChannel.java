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

package io.cozmic.channels.HTTP;

import io.cozmic.CozmicMessage;
import io.cozmic.ObjectBytes;
import org.json.JSONObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by Eric on 4/22/2014.
 */
public class HTTPChannel extends AbstractHTTPChannel {

    @Override
    public void start() {
        super.start();
        httpServer.requestHandler(new HttpServerRequestHandler());
        httpServer.listen(port, host);
        logger.info(config.getString("name") + " listening on port: " + port);

    }

    public class HttpServerRequestHandler implements Handler<HttpServerRequest> {
        @Override
        public void handle(final HttpServerRequest request) {

            request.expectMultiPart(false);

            request.bodyHandler(new BodyHandler(request));

            SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
            dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));

            request.headers().add("Channel-Time-Received", dateFormatUTC.format(new Date()));

            request.response().setChunked(true);
            request.response().setStatusCode(200).setStatusMessage("OK").end("Event Received");

        }
    }


    public class BodyHandler implements Handler<Buffer> {
        private final HttpServerRequest request;

        public BodyHandler(HttpServerRequest request) {
            this.request = request;
        }

        @Override
        public void handle(Buffer body) {
            final Map<String, Object> originalRequest = new HashMap<>();
            // The entire body has now been received

            byte[] messageBytes = null;
            JSONObject serverRequest = HTTPRequestSerializer.jsonize(request);

            try {
                originalRequest.put("request", ObjectBytes.getBytes(serverRequest));
                originalRequest.put("body", body.getBytes());
                originalRequest.put("source", config.getString("name"));

                messageBytes = ObjectBytes.getBytes(originalRequest);
                CozmicMessage cm = new CozmicMessage(messageBytes);

                eb.send(conduitAddress, cm.toBytes());

            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }

        }
    }

}
