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

package io.cozmic.emanator;

/**
 * Created by Eric on 4/2/2014.
 */


import io.cozmic.CozmicComponent;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.*;
import org.vertx.java.core.json.JsonArray;

import java.util.Set;



public class Emanator extends CozmicComponent {
    public Set<String> conduits;

    public void start() {
        super.start();

        JsonArray conduitStartList = config.getArray("conduits");

        conduits = vertx.sharedData().getSet("emanator.conduits");


        for (int i = 0; i < conduitStartList.toArray().length; i++) {
            String conduit = conduitStartList.toArray()[i].toString();
            conduits.add(conduit);
        }

        Handler<Message<byte[]>> emanationHandler = new EmanationMessageHandler();

        eb.registerHandler(config.getString("inbound_address"), emanationHandler);


        container.deployWorkerVerticle(EmanatorQueueListener.class.getName(), config.getObject("emanator_queue_listener"), 1, true);
        //container.deployVerticle(EmanatorQueueListener.class.getName(), config.getObject("emanator_queue_listener"), 1);
        System.out.println("Emanator Queue Listener Started");

    }

    @Override
    public void onConfigUpdate() {

        JsonArray conduitStartList = config.getArray("conduits");
        conduits = vertx.sharedData().getSet("emanator.conduits");
        conduits.clear();

        for (int i = 0; i < conduitStartList.toArray().length; i++) {
            String conduit = conduitStartList.toArray()[i].toString();
            conduits.add(conduit);
        }

        configPendingUpdate = false;

    }


    private class EmanationMessageHandler implements Handler<Message<byte[]>> {
        public void handle(Message<byte[]> message) {
            emanate(message);
        }

        public void emanate (Message<byte[]> message) {
            for (String conduit:conduits) {
                eb.send(conduit, message.body());
            }
        }
    }
}
