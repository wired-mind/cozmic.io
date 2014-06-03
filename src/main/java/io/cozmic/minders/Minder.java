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

package io.cozmic.minders;


import io.cozmic.CozmicComponent;
import io.cozmic.CozmicMessage;
import io.cozmic.JSONFileLoader;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.json.JsonObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Eric on 4/22/2014.
 */
public class Minder extends CozmicComponent implements IMinder {
    private final ConcurrentLinkedQueue<CozmicMessage> failedMessageBuffer = new ConcurrentLinkedQueue<>();
    public Map<String, NormalizationDictionary> normalizationDictionary = new HashMap<>();
    private final Map<String, Integer> processingMap = new HashMap<>();
    private ProcessingModel processModelObject;
    private final Map<String, Date> FileChangeMap = new ConcurrentHashMap<>();
    private long requeueTimerID = -1;
    private final long requeueWait = 5000;
    private String inProcessAddress;
    private String journalAddress;
    private String normalizationAddress;
    private String preceptsDirectory;
    private String preceptsFile;
    private boolean stillPolling = false;
    public long replyTimeout = 90000L;


    @Override
    public void start() {
        super.start();
        inboundAddress = config.getString("conduit_address");
        failuresAddress = config.getString("failures_address");
        inProcessAddress = config.getString("inprocess_address");
        journalAddress = config.getString("journal_address");
        normalizationAddress = config.getString("normalization_address");
        preceptsDirectory = config.getString("precepts_directory");
        preceptsFile = config.getString("precept_set");

        JsonObject journalerConfig = config.getObject("journal_datastore");
        journalerConfig.putString("journal_address", journalAddress);
        journalerConfig.putString("owner", name);

        container.deployWorkerVerticle("io.cozmic.minders.JournalDatastore", journalerConfig, 1, false, new AsyncResultHandler<String>() {
            public void handle(AsyncResult<String> deployResult) {
                if (deployResult.succeeded()) {
                    String deploymentId = deployResult.result();
                    Registrar.put("Emanator.queue", deploymentId);
                    logger.info("Emanator Queue Service was deployed.");

                } else {
                    logger.error(deployResult.cause().getMessage());
                }
            }
        });


        String file = preceptsDirectory + preceptsFile;

        try {
            FileChangeMap.put(file, new Date());
            JsonObject preceptObject = JSONFileLoader.LoadJSONFile(file);
            processModelObject = new ProcessingModel(preceptObject);

            final String preceptName = preceptObject.getString("name");
            String processType = preceptObject.getString("type");
            preceptObject = null;
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }


        //This is where we check for config file changes and update the components

        long pollingTimerId = vertx.setPeriodic(5000, new Handler<Long>() {
            public void handle(Long timerID) {
                if (!stillPolling) {
                    pollFiles();
                }
            }
        });


        //handle messages from the primary Conduit to the primary inbound address
        @SuppressWarnings("unchecked")
        Handler<Message<byte[]>> inboundMessageHandler = new InboundMessageHandler();

        @SuppressWarnings("unchecked")
        Handler<Message<byte[]>> normalizationMessageHandler = new MessageNormalizationHandler();

        //handle messages that need to have the next processing step looked up and then forward message.
        //CozmicMessages can also have a processing map attached so they can be forwarded between Instruments.
        @SuppressWarnings("unchecked")
        Handler<Message<byte[]>> inProcessMessageHandler = new MessageProcessingHandler();
        //handle messages that were returned because of failures somewhere im processing
        @SuppressWarnings("unchecked")
        Handler<Message<byte[]>> failedMessageHandler = new FailedMessageHandler();

        eb.registerHandler(inboundAddress, inboundMessageHandler);
        eb.registerHandler(failuresAddress, failedMessageHandler);
        eb.registerHandler(inProcessAddress, inProcessMessageHandler);
        eb.registerHandler(normalizationAddress, normalizationMessageHandler);

    }


    CozmicMessage normalizeData(final CozmicMessage cm) {
        //just set normalized data to current data
        //This would be the place to use a rosetta to convert the data the update
        // the CozmicMessage

        try {
            cm.normalizedData(cm.data());
            cm.currentData(cm.normalizedData());
            //log.info("current data size: " + cm.currentData().length);

        } catch (Exception ex) {
            logger.error("error normalizing");
            logger.error(ex.getMessage());

        }

        return cm;
    }


    void pollFiles() {
        stillPolling = true;
        final String file = preceptsDirectory + preceptsFile;

        vertx.fileSystem().props(file, new AsyncResultHandler<FileProps>() {
            public void handle(AsyncResult<FileProps> ar) {
                if (ar.succeeded()) {
                    try {
                        if (ar.result().lastModifiedTime().after(FileChangeMap.get(file))) {
                            logger.info(file + " has changed");
                            FileChangeMap.put(file, new Date());

                            JsonObject preceptObject = JSONFileLoader.LoadJSONFile(file);
                            processModelObject = new ProcessingModel(preceptObject);
                            logger.info("Processing Model updated");
                            preceptObject = null;
                        }

                    } catch (NullPointerException ex) {
                        //

                    } catch (Exception ex) {
                        logger.error(ex.getMessage());
                    }
                } else {
                    logger.error("Failed to get props", ar.cause());
                }
            }
        });
        stillPolling = false;
    }

    private class InProcessResponseHandler implements Handler<AsyncResult<Message<Boolean>>> {
        private final CozmicMessage cm;
        private final ProcessingModel.Step step;
        private final int lastStep;

        public InProcessResponseHandler(CozmicMessage cm, ProcessingModel.Step step, int lastStep) {
            this.cm = cm;
            this.step = step;
            this.lastStep = lastStep;
        }

        public void handle(AsyncResult<Message<Boolean>> result) {
            if (result.succeeded()) {
                processingMap.put(cm.messageUUID(), step.increment(lastStep));

                if (!step.action.equals("complete")) {
                    eb.send(inProcessAddress, cm.toBytes());
                }
            } else {
                logger.error(result.cause());
                ReplyException ex = (ReplyException) result.cause();

                if (ex.failureCode() == ResponseType.FailWithRetry.value()) {
                    logger.info("fail with retry indicated. Requeuing message.");
                    //processingMap.put(cm.messageUUID(), lastStep);
                    eb.send(failuresAddress, cm.toBytes());

                } else if (ex.failureCode() == ResponseType.FailNoRetry.value()) {
                    logger.info("fail with No retry indicated. Disregarding message.");
                    processingMap.remove(cm.messageUUID());
                }
            }
        }
    }

    private class MessageProcessingHandler implements Handler<Message<byte[]>> {
        public void handle(final Message<byte[]> message) {
            final CozmicMessage cm = CozmicMessage.fromBytes(message.body());

            int stepNum = 0;

            if (processingMap.containsKey(cm.messageUUID())) {
                stepNum = processingMap.get(cm.messageUUID());
            }

            final int lastStep = stepNum;
            final ProcessingModel.Step step = processModelObject.nextStep(lastStep);
            if (step != null) {
                final String nextAddress = step.address;

                if (processModelObject.isFinished(lastStep) || nextAddress.equals("complete")) {
                    processingMap.remove(cm.messageUUID());
                    logger.info("message: " + cm.messageUUID() + " processing complete");

                } else {
                    if (step.needsReply) {
                        eb.sendWithTimeout(nextAddress, cm.toBytes(), replyTimeout, new InProcessResponseHandler(cm, step, lastStep));

                    } else {
                        eb.send(nextAddress, cm.toBytes());
                        processingMap.put(cm.messageUUID(), step.increment(lastStep));

                        if (!step.action.equals("complete")) {
                            eb.send(inProcessAddress, cm.toBytes());

                        } else {
                            processingMap.remove(cm.messageUUID());
                        }
                    }
                }
            }
        }
    }


    private class FailedMessageHandler implements Handler<Message<byte[]>> {
        public void handle(final Message<byte[]> message) {
            final CozmicMessage cm = CozmicMessage.fromBytes(message.body());

            if (cm != null) {
                int stepNum = 0;

                if (processingMap.containsKey(cm.messageUUID())) {
                    stepNum = processingMap.get(cm.messageUUID());
                }

                processingMap.put(cm.messageUUID(), stepNum);
                //if message fail with retry
                failedMessageBuffer.add(cm);
                //reprocess failed messages that were returned
                if (requeueTimerID == -1) {
                    requeueTimerID = vertx.setTimer(requeueWait, new Handler<Long>() {
                        public void handle(Long timerID) {
                            Iterator<CozmicMessage> bufferIterator = failedMessageBuffer.iterator();

                            while (bufferIterator.hasNext()) {
                                CozmicMessage message = bufferIterator.next();
                                eb.send(inProcessAddress, message.toBytes());
                                bufferIterator.remove();
                            }
                            bufferIterator = null;
                            requeueTimerID = -1;
                            vertx.cancelTimer(requeueTimerID);
                        }
                    });
                }
            }
        }
    }

    private class MessageNormalizationHandler implements Handler<Message<byte[]>> {
        public void handle(final Message<byte[]> message) {
            CozmicMessage cm = CozmicMessage.fromBytes(message.body());

            if (cm != null) {
                try {
                    cm = normalizeData(cm);
                    message.reply(cm.toBytes());

                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                    message.fail(1099, "Data normalization failed");

                }
            } else {
                message.fail(1098, "Data normalization failed, NULL data");
            }
        }
    }


    private class InboundMessageHandler implements Handler<Message<byte[]>> {
        public void handle(final Message<byte[]> message) {
            ;
            final CozmicMessage cm = CozmicMessage.fromBytes(message.body());
            processingMap.put(cm.messageUUID(), 0);

            //Journal message
            eb.send(journalAddress, cm.toBytes());

            eb.sendWithTimeout(normalizationAddress, cm.toBytes(), replyTimeout, new Handler<AsyncResult<Message<byte[]>>>() {
                public void handle(AsyncResult<Message<byte[]>> result) {
                    if (result.succeeded()) {
                        eb.send(inProcessAddress, cm.toBytes());

                    } else {
                        logger.error(result.cause());
                        eb.send(failuresAddress, cm.toBytes());
                    }
                }
            });
        }
    }
}
