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

import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IQueue;

import io.cozmic.conduits.HazelcastConduit;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

import java.util.Collection;
import java.util.LinkedList;


/**
 * Created by Eric on 4/18/2014.
 */
public class EmanatorQueueListener extends HazelcastConduit {

    private IQueue<byte[]> distributionQueue;
    private final Collection<byte[]> requeueBuffer = new LinkedList<>();

    private long pollTimerID = -1;
    private long requeueTimerID = -1;
    private final long requeueWait = 100000;
    private final int period = 1;
    private final int maxItems = 100;
    private static final boolean useBatch = true;
    private boolean possibleRoom = false;


    public void start() {

        super.start();
        final String outboundAddress = config.getString("outbound_address");
        final String requeueAddress = config.getString("requeue_message_address");
        final String queueName = config.getString("queue_name");


        distributionQueue = hazelcast.getQueue(queueName);

        final Handler<Long> pollingProcess = new QueuePollingHandler(outboundAddress);

        //Async polling of the queue
        pollTimerID = vertx.setTimer(period, pollingProcess);

        //If anything down the line needs a message to be reprocessed it can send to be requeued
        Handler<Message<byte[]>> requeueHandler = new MessageReQueueHandler();

        eb.registerHandler(requeueAddress, requeueHandler);
    }


    private class QueuePollingHandler implements Handler<Long> {
        private final String outboundAddress;

        public QueuePollingHandler(String outboundAddress) {
            this.outboundAddress = outboundAddress;
        }

        public void handle(Long timerID) {
            if (useBatch) {
                Collection<byte[]> queueBuffer = new LinkedList<>();

                try {
                    distributionQueue.drainTo(queueBuffer, maxItems);
                    possibleRoom = true;
                    for (byte[] queueItem : queueBuffer) {
                        eb.send(outboundAddress, queueItem);
                    }
                } catch (HazelcastInstanceNotActiveException ex) {
                    //
                } catch (Exception ex) {
                    // do something with data that couldn't be sent
                    System.out.println(ex.getMessage());

                } finally {
                    queueBuffer = null;
                }

            } else {

                try {
                    byte[] queueItem = distributionQueue.take();
                    possibleRoom = true;
                    eb.send(outboundAddress, queueItem);
                } catch (InterruptedException ex) {
                    //
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }

            }
            pollTimerID = vertx.setTimer(period, this);
        }
    }

    private class MessageReQueueHandler implements Handler<Message<byte[]>> {
        public void handle(final Message<byte[]> message) {

            final byte[] requeueItem = message.body();

            boolean requeued = distributionQueue.offer(requeueItem);

            if (!requeued) {
                possibleRoom = false;
                requeueBuffer.add(requeueItem);

                //wait and try again but don't block
                if (requeueTimerID == -1) {
                    requeueTimerID = vertx.setTimer(requeueWait, new Handler<Long>() {
                        public void handle(Long timerID) {
                            if (possibleRoom) {
                                boolean complete = true;
                                for (byte[] item : requeueBuffer) {
                                    boolean requeued = distributionQueue.offer(item);
                                    if (requeued) {
                                        requeueBuffer.remove(item);
                                    } else {
                                        complete = false;
                                    }
                                }
                                if (complete) {
                                    vertx.cancelTimer(requeueTimerID);
                                    requeueTimerID = -1;
                                }
                            }
                        }
                    });
                }
            }

        }
    }
}
