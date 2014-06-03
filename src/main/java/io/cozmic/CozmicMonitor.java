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


import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.shareddata.SharedData;
import org.vertx.java.platform.Verticle;

import org.apache.commons.io.FilenameUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class CozmicMonitor extends Verticle {
    public final static List<String> cozmicComponents = Arrays.asList("emanator", "channels", "instruments", "minders");
    private final Map<String, Map<String, JsonObject>> cozmicConfigsMap = new ConcurrentHashMap<>();
    private final Map<String, Date> fileChangeMap = new ConcurrentHashMap<>();
    private final Map<String, String> componentFileMap = new ConcurrentHashMap<>();
    private Map<String, String> Registrar;
    private Logger log;
    private String configsDirectory;
    private EventBus eventbus;
    private boolean stillPolling = false;

    @Override
    public void start() {
        log = container.logger();
        JsonObject startConfig = container.config();
        eventbus = vertx.eventBus();
        SharedData sharedData = vertx.sharedData();
        Registrar = sharedData.getMap("Registrar");
        JsonObject editorConfig;
        JsonObject bridgeConfig;
        JsonObject queueConfig;


        try {
            String queueDirectory = "";
            queueDirectory = startConfig.getString("cozmic_components");
            queueConfig = JSONFileLoader.LoadJSONFile(queueDirectory + "\\Emanator\\Emanator.json").getObject("Emanator").getObject("emanator_queue_service");

            container.deployWorkerVerticle("io.cozmic.emanator.queue.EmanatorQueueService", queueConfig, 2, false, new AsyncResultHandler<String>() {
                public void handle(AsyncResult<String> deployResult) {
                    if (deployResult.succeeded()) {
                        String deploymentId = deployResult.result();
                        Registrar.put("Emanator.queue", deploymentId);
                        log.info("Emanator Queue Service was deployed.");

                    } else {
                        log.error(deployResult.cause().getMessage());
                    }
                }
            });

        } catch (Exception ex) {
            log.error(ex.getMessage());

        }


        try {
            String editorDirectory = startConfig.getString("cozmic_editor");
            editorConfig = JSONFileLoader.LoadJSONFile(editorDirectory + "/conf.json");
            final String editorName = editorConfig.getString("name");

            container.deployVerticle("io.cozmic.web.WebServer", editorConfig, 1, new AsyncResultHandler<String>() {
                public void handle(AsyncResult<String> deployResult) {
                    if (deployResult.succeeded()) {
                        String deploymentId = deployResult.result();
                        Registrar.put(editorName, deploymentId);
                        log.info("Editor Service was deployed.");

                    } else {
                        deployResult.cause().printStackTrace();
                    }
                }
            });

        } catch (Exception ex) {
            log.error(ex.getMessage());

        }

        try {
            String bridgeDirectory = startConfig.getString("cozmic_bridge");
            bridgeConfig = JSONFileLoader.LoadJSONFile(bridgeDirectory + "/conf.json");
            final String bridgeName = bridgeConfig.getString("name");

            container.deployVerticle("io.cozmic.web.WebServer", bridgeConfig, 1, new AsyncResultHandler<String>() {
                public void handle(AsyncResult<String> deployResult) {
                    if (deployResult.succeeded()) {
                        String deploymentId = deployResult.result();
                        Registrar.put(bridgeName, deploymentId);
                        log.info("Bridge Service was deployed.");
                    } else {
                        deployResult.cause().printStackTrace();
                    }
                }
            });

        } catch (Exception ex) {
            log.error(ex.getMessage());

        }

        configsDirectory = startConfig.getString("cozmic_components");

        String[] directories = vertx.fileSystem().readDirSync(configsDirectory);

        for (String dir : directories) {

            final String name = FilenameUtils.getName(dir.toLowerCase());

            if (cozmicComponents.contains(name)) {
                Map<String, JsonObject> configsMap = new HashMap<>();
                String[] fileRead = vertx.fileSystem().readDirSync(dir, ".*\\.json");

                if (fileRead.length > 0) {
                    for (String file : fileRead) {
                        try {
                           //log.info("File name: " + file);

                            JsonObject conf = JSONFileLoader.LoadJSONFile(file);
                            String componentName = (String) conf.getFieldNames().toArray()[0];

                            configsMap.put(componentName, conf);
                            componentFileMap.put(file, componentName);
                            fileChangeMap.put(file, new Date());

                        } catch (Exception ex) {
                            log.error(ex.getMessage());
                        }
                    }
                    cozmicConfigsMap.put(name, configsMap);
                } else {
                    log.error("Failed to read " + configsDirectory);
                    cozmicConfigsMap.put(name, configsMap);
                }
            }
        }

        System.out.println("Deploying Emanator, Channels, Minders and Instruments");

        for (String component : cozmicComponents) {
            log.info("[ " + component.toUpperCase() + " ]");
            Map<String, JsonObject> componentConfigs = cozmicConfigsMap.get(component);

            if (componentConfigs != null) {
                for (Map.Entry componentConfig : componentConfigs.entrySet()) {
                    JsonObject thisConfig = (JsonObject) componentConfig.getValue();

                    final String componentName = (String) thisConfig.getFieldNames().toArray()[0];
                    JsonObject componentConfigObject = thisConfig.getObject(componentName);
                    String componentClass = componentConfigObject.getString("class");
                    int instances = componentConfigObject.getInteger("instances", 1);

                    container.deployVerticle(componentClass, componentConfigObject, instances, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> deployResult) {
                            if (deployResult.succeeded()) {
                                String deploymentId = deployResult.result();
                                Registrar.put(componentName, deploymentId);

                            } else {
                                deployResult.cause().printStackTrace();
                            }
                        }
                    });
                    log.info(componentName + " was deployed.");

                }
            }
        }

        long pollingTimerId = vertx.setPeriodic(5000, new Handler<Long>() {
            public void handle(Long timerID) {
                if (!stillPolling) {
                    pollFiles();
                }
            }
        });

    }


    public void pollFiles() {
        stillPolling = true;
        String[] directories = vertx.fileSystem().readDirSync(configsDirectory);

        for (String dir : directories) {
            final Map<String, JsonObject> configsMap = new HashMap<>();
            final String name = FilenameUtils.getName(dir.toLowerCase());

            if (cozmicComponents.contains(name)) {
                String[] fileRead = vertx.fileSystem().readDirSync(dir, ".*\\.json");

                //undeploy verticles that have had files removed
                for (final String mappedFile : fileChangeMap.keySet()) {
                    vertx.fileSystem().exists(mappedFile, new AsyncResultHandler<Boolean>() {
                        public void handle(AsyncResult<Boolean> ar) {
                            if (!ar.result()) {
                                final String componentName = componentFileMap.get(mappedFile);
                                String deploymentId = Registrar.get(componentName);
                                configsMap.remove(componentName);
                                componentFileMap.remove(mappedFile);
                                fileChangeMap.remove(mappedFile);
                                undeployVerticle(componentName, deploymentId);
                            }
                        }

                    });

                    for (final String file : fileRead) {
                        vertx.fileSystem().props(file, new AsyncResultHandler<FileProps>() {
                            public void handle(AsyncResult<FileProps> ar) {
                                if (ar.succeeded()) {
                                    try {
                                        if (ar.result().lastModifiedTime().after(fileChangeMap.get(file))) {
                                            log.info(file + " has changed or is new");
                                            fileChangeMap.put(file, new Date());

                                            final JsonObject conf = JSONFileLoader.LoadJSONFile(file);
                                            final String componentName = (String) conf.getFieldNames().toArray()[0];
                                            final String configAddress = conf.getObject(componentName).getString("owner_address", "");

                                            if (!configAddress.equals("")) {
                                                log.info("component: " + componentName + " to be updated");
                                                final JsonObject componentConfigObject = conf.getObject(componentName);
                                                configsMap.put(componentName, componentConfigObject);
                                                eventbus.send(configAddress, componentConfigObject, new Handler<Message<Boolean>>() {
                                                    public void handle(Message<Boolean> message) {
                                                        if (message.body()) {
                                                            String deploymentId = Registrar.get(componentName);
                                                            redeployVerticle(componentName, deploymentId, componentConfigObject);
                                                        }
                                                    }
                                                });

                                                log.info("sent message to : " + configAddress);
                                            } else if (!fileChangeMap.containsKey(file)) {
                                                configsMap.put(componentName, conf);
                                                componentFileMap.put(file, componentName);
                                                deployVerticle(componentName,  conf.getObject(componentName));
                                            }
                                        }

                                    } catch (NullPointerException ex) {
                                        //

                                    } catch (Exception ex) {
                                        log.error(ex.getMessage());
                                    }

                                } else {
                                    log.error("Failed to get props", ar.cause());
                                }
                            }
                        });
                    }
                }
                cozmicConfigsMap.put(name, configsMap);
            }
            stillPolling = false;
        }
    }


    public void redeployVerticle(final String componentName, final String deploymentId, final JsonObject config) {
        container.undeployVerticle(deploymentId, new Handler<AsyncResult<Void>>() {
            public void handle(AsyncResult<Void> undeployResult) {
                if (undeployResult.succeeded()) {
                    log.info("undeployed " + componentName);
                    Registrar.remove(componentName);

                    String componentClass = config.getString("class");
                    int instances = config.getInteger("instances", 1);

                    container.deployVerticle(componentClass, config, instances, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> deployResult) {
                            if (deployResult.succeeded()) {
                                String deploymentId = deployResult.result();
                                Registrar.put(componentName, deploymentId);
                                log.info("Redeployed " + componentName);

                            } else {

                                deployResult.cause().printStackTrace();
                            }
                        }
                    });

                } else {

                    undeployResult.cause().printStackTrace();
                }
            }
        });
    }

    public void deployVerticle(final String componentName, final JsonObject config) {

        String componentClass = config.getString("class");
        int instances = config.getInteger("instances", 1);

        container.deployVerticle(componentClass, config, instances, new AsyncResultHandler<String>() {
            public void handle(AsyncResult<String> deployResult) {
                if (deployResult.succeeded()) {
                    String deploymentId = deployResult.result();
                    Registrar.put(componentName, deploymentId);
                    log.info("Deployed " + componentName);

                } else {

                    deployResult.cause().printStackTrace();
                }
            }
        });
    }

    public void undeployVerticle(final String componentName, final String deploymentId) {
        container.undeployVerticle(deploymentId, new Handler<AsyncResult<Void>>() {
            public void handle(AsyncResult<Void> undeployResult) {
                if (undeployResult.succeeded()) {
                    log.info("undeployed " + componentName);
                    Registrar.remove(componentName);

                } else {

                    undeployResult.cause().printStackTrace();
                }
            }
        });
    }

}