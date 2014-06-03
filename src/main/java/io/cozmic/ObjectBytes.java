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

import org.json.JSONObject;
import org.vertx.java.core.json.JsonObject;

import java.io.*;

/**
 * Created by Eric on 4/27/2014.
 */
public final class ObjectBytes {
    // Convert Object to byte array
    public static byte[] getBytes(Object object) throws IOException {
        byte[] bytes = null;

        try{
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(output);
            objectStream.writeObject(object);
            bytes = output.toByteArray();

        } catch (Exception ex) {
            System.out.print(ex.getMessage());
            throw  new IOException();
        }

        return bytes;
    }

    public static byte[] getBytes(JSONObject jsonObject) throws IOException {
        byte[] bytes = null;

        try {
            bytes = jsonObject.toString().getBytes("utf-8");

        } catch (Exception ex) {
            System.out.print(ex.getMessage());
            throw  new IOException();
        }

        return bytes;
    }


    public static Object getObject(byte[] bytes) throws IOException, ClassNotFoundException {
        Object object;

        try {
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            ObjectInputStream objectStream = new ObjectInputStream(input);

            object = objectStream.readObject();

        } catch (Exception ex) {
            System.out.print(ex.getMessage());
            throw new IOException();

        }

        return object;

    }

    public static JSONObject getJSONObject(byte[] bytes) throws IOException, ClassNotFoundException {
        JSONObject object;

        try {
            String jsonString = new String(bytes, "UTF-8");
            object = new JSONObject(jsonString);

        } catch (Exception ex) {
            System.out.print("getJSONObject -" + ex.getMessage());
            throw new IOException();

        }

        return object;

    }


}
