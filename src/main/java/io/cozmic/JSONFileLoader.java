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

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Created by Eric on 5/5/2014.
 */
public final class JSONFileLoader {

    public static JsonObject LoadJSONFile(String configFileLocation) throws DecodeException, FileNotFoundException  {

        JsonObject config;

        if (configFileLocation != null) {
            try (Scanner scanner = new Scanner(new File(configFileLocation)).useDelimiter("\\A")){
                String sconf = scanner.next();
                try {
                    config = new JsonObject(sconf);
                } catch (DecodeException ex) {
                    System.out.println("Configuration file does not contain a valid JSON object");
                    throw ex;
                }
            } catch (FileNotFoundException ex) {
                System.out.println("Config file " + configFileLocation + " does not exist");
                throw ex;
            }
        } else {
            config = null;
        }

        return config;
    }
}
