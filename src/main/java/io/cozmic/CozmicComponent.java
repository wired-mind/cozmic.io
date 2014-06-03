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

package io.cozmic;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Eric on 5/8/2014.
 */
public abstract class CozmicComponent extends BusModBase implements ICozmicComponent {
    public boolean isThreaded = false;
    protected Map<String, String> Registrar;
    public String name;
    public boolean configPendingUpdate;
    private String componentId;
    private String configAddress;
    private final boolean redeployOnConfigChange = true;
    private final boolean waitForEmptyBuffer = false;
    private final List<byte[]> requeueBuffer = Collections.synchronizedList(new LinkedList<byte[]>());
    public String inboundAddress;
    public String failuresAddress;

    @Override
    public void start() {
        super.start();
        try {
            configPendingUpdate = false;

            configAddress = config.getString("owner_address");
            name = config.getString("name", configAddress);
            inboundAddress = config.getString("inbound_address");
            failuresAddress = config.getString("failures_address");
            eb = vertx.eventBus();
            logger = container.logger();
            Registrar = vertx.sharedData().getMap("Registrar");


            Handler<Message<JsonObject>> configChangeHandler = new ConfigChangeHandler();

            if (configAddress != null) {
                eb.registerHandler(configAddress, configChangeHandler);
            }

            setComponentId(retrieveComponentId());

        } catch (Exception ex) {

            logger.error(ex.getMessage());
            throw ex;

        }

    }


    public void onConfigUpdate() {

        this.configPendingUpdate = false;

    }


    public void updateConfig(JsonObject config) {

        this.config = config;

    }

    public String getComponentId() {

        return this.componentId;

    }

    public void setComponentId(String componentId) {

        this.componentId = componentId;
    }

    public String retrieveComponentId() {

        return Registrar.get(this.config.getString("name", this.getClass().getName()));

    }

    private class ConfigChangeHandler implements Handler<Message<JsonObject>> {
        public void handle(final Message<JsonObject> message) {
            updateConfig(message.body());
            configPendingUpdate = true;

            if (waitForEmptyBuffer) {
                long timerID = vertx.setPeriodic(1000, new Handler<Long>() {
                    public void handle(Long timerID) {
                        if (requeueBuffer.size() == 0) {
                            message.reply(redeployOnConfigChange);
                            onConfigUpdate();
                            vertx.cancelTimer(timerID);
                        }
                    }
                });

            } else {
                message.reply(redeployOnConfigChange);
                onConfigUpdate();

            }
        }
    }
}
