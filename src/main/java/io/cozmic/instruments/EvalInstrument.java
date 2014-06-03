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

package io.cozmic.instruments;

import io.cozmic.CozmicComponent;
import org.mvel2.MVEL;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Eric on 4/25/2014.
 */
public abstract class EvalInstrument extends CozmicComponent implements Instrument {

    public static boolean isThreaded = false;

    @Override
    public void start() {
        super.start();
        String expression = config.getString("expression", "");


        Handler<Message<JsonObject>> analysisHandler = new Handler<Message<JsonObject>>() {
            public void handle(final Message<JsonObject> message) {
                JsonObject analysisConfig = message.body();
                JsonObject variables = analysisConfig.getObject("variables");
                String expression = analysisConfig.getString("expression");
                String returnType = analysisConfig.getString("return_type");

                Map varMap = new HashMap();
                Set<String> varNames = variables.getFieldNames();

                for(String var : varNames) {
                    varMap.put(var, analysisConfig.getValue(var));
                }

                Class returnTypeClass = getReturnType(returnType);
                Object result = MVEL.eval(expression, varMap);
                message.reply(returnTypeClass.cast(result));
            }

        };

        eb.registerHandler(inboundAddress, analysisHandler);

    }

    public Class getReturnType(String typeName) {

        Class returnTypeClass = null;

        try {
            returnTypeClass = Class.forName(typeName);


        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

        return returnTypeClass;

    }

}

