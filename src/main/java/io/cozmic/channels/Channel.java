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

package io.cozmic.channels;


import io.cozmic.CozmicComponent;
import io.cozmic.conduits.EmanatorQueueConduit;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonObject;


/**
 * Created by Eric on 4/29/2014.
 */
public abstract class Channel extends CozmicComponent {
    protected String host;
    protected int port;
    public String conduitAddress;
    private String conduitOwnerName;


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
    }

}