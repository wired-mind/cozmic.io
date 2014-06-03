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

package io.cozmic.conduits;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;


/**
 * Created by Eric on 4/22/2014.
 */

public class EmanatorQueueConduit extends HazelcastConduit {


    public void start() {

        super.start();
        try {
            final com.hazelcast.client.config.ClientConfig clientConfig = new ClientConfig();

            clientConfig.getGroupConfig().setName(clusterName).setPassword(clusterPassword);
            clientConfig.getNetworkConfig().addAddress(address);

            final HazelcastInstance hazelcast = HazelcastClient.newHazelcastClient(clientConfig);
            final IQueue<byte[]> distributionQueue = hazelcast.getQueue(queueName);

            Handler<Message<byte[]>> queueItemHandler = new QueueItemHandler(distributionQueue);

            eb.registerHandler(inboundAddress, queueItemHandler);

        } catch (Exception ex) {
            logger.info("Emanator Queue Conduit failed to start");
        }
    }

    private static class QueueItemHandler implements Handler<Message<byte[]>> {
        private final IQueue<byte[]> distributionQueue;

        public QueueItemHandler(IQueue<byte[]> distributionQueue) {
            this.distributionQueue = distributionQueue;
        }

        public void handle(Message<byte[]> message) {
            try {
                distributionQueue.put(message.body());

            } catch (Exception ex) {
                System.out.println("Failed to place message on Hazelcast Queue");
                System.out.println(ex.getMessage());
            }

        }
    }
}

