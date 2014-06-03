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

package io.cozmic.minders;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.*;

/**
 * Created by Eric on 5/20/2014.
 */
public class ProcessingModel {
    private Map<String, Step> stepMap;
    private Map<Integer, Step> processList = new HashMap<>();
    private int activeStepCount = 0;
    public String processType;


    public ProcessingModel(JsonObject preceptObject) {
        final String preceptName = preceptObject.getString("name");
        processType = preceptObject.getString("type");

        switch (processType) {
            case "ordered-process-list":
                final JsonArray steps = preceptObject.getArray("precepts");
                setSteps(steps);
                break;

            case "flow":

                break;

            case "mapped-process":
                break;

            default:
                break;

        }
    }

    public void setSteps(JsonArray steps) {
        Iterator<Object> stepSetIterator = steps.iterator();

        while (stepSetIterator.hasNext()) {
            JsonObject precept = (JsonObject) stepSetIterator.next();

            int order = precept.getInteger("order_id");
            String instrument = precept.getString("instrument");
            String type = precept.getString("instrument_type");
            String address = precept.getString("instrument_address");
            String action = precept.getString("action");

            addStep(precept);
        }
    }

    public int getActiveStepCount() {
        return this.activeStepCount;

    }

    public String nextAddress(int lastStep) {
        if (processList.containsKey(lastStep + 1)) {
            Step nextStep = processList.get(lastStep + 1);

            return nextStep.address;
        }

        return "complete";
    }

    public Step nextStep(int lastStep) {
        Step nextStep = null;

        if (processList.containsKey(lastStep + 1)) {
            nextStep = processList.get(lastStep + 1);
        }

        return nextStep;
    }

    public void addStep(JsonObject precept) {
        int order = precept.getInteger("order_id");
        String instrument = precept.getString("instrument");
        String type = precept.getString("instrument_type");
        String address = precept.getString("instrument_address");
        String action = precept.getString("action");
        boolean needsReply = precept.getBoolean("needs_reply", false);


        Step step = new Step(instrument, type, address, action, needsReply);

        processList.put(order, step);
        activeStepCount++;

    }

    public boolean isFinished(int lastStep) {
        boolean complete = true;

        if (processList.containsKey(lastStep + 1)) {
            complete = false;
        }

        return complete;
    }


    public class Step {
        final public String instrument;
        final public String instrumentType;
        final public String address;
        final public String action;
        final public boolean needsReply;

        Step(String instrument, String instrumentType, String address, String action, boolean needsReply) {
            this.instrument = instrument;
            this.address = address;
            this.action = action;
            this.instrumentType = instrumentType;
            this.needsReply = needsReply;
        }

        public int increment(int lastStep) {
            return lastStep + 1;
        }

        public int decrement(int lastStep) {
            return lastStep - 1;
        }

    }

}
