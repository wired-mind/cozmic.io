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


import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Eric on 5/10/2014.
 */
public class CozmicMessage implements Serializable, Cloneable {
    private byte[] data;
    private JsonObject processMap;
    private byte[] normalizedData;
    private byte[] currentData;
    private int lastProcessMapStep;
    private String messageUUID;


    public CozmicMessage() {
        this.data = null;
        this.processMap = new JsonObject();
        this.messageUUID = UUID.randomUUID().toString();
    }

    public CozmicMessage(byte[] data) {
        this.data = data;
        this.processMap = new JsonObject();
        this.messageUUID = UUID.randomUUID().toString();
    }

    public CozmicMessage(Buffer data) {
        this.data = data.getBytes();
        this.processMap = new JsonObject();
        this.messageUUID = UUID.randomUUID().toString();
    }

    public CozmicMessage(byte[] data, JsonObject processMap) {

        this.data = data;
        this.processMap = processMap;
        this.messageUUID = UUID.randomUUID().toString();
    }

    @Override
    public CozmicMessage clone() {
        try {
            CozmicMessage cm = (CozmicMessage) super.clone();
            cm.data(this.data().clone());
            cm.processMap(this.processMap().copy());
            cm.normalizedData(this.normalizedData().clone());
            cm.currentData(this.currentData().clone());
            cm.lastProcessMapStep(this.lastProcessMapStep());
            cm.messageUUID(this.messageUUID());

            return cm;

        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    public String messageUUID() {

        return this.messageUUID;
    }

    private void messageUUID(String messageUUID) {

        this.messageUUID = messageUUID;
    }

    public void data(byte[] data) {
        this.data = data;
    }

    public void normalizedData(byte[] normalizedData) {
        this.normalizedData = normalizedData;
    }

    public void currentData(byte[] currentData) {
        this.currentData = currentData;
    }

    public byte[] data() {
        return this.data;
    }

    public byte[] normalizedData() {
        return this.normalizedData;
    }

    public byte[] currentData() {
        return this.currentData;
    }

    public JsonObject processMap() {
        return this.processMap;
    }

    public void processMap(JsonObject processMap) {
        this.processMap = processMap();
    }

    public void lastProcessMapStep(int stepNumber) {
        this.lastProcessMapStep = stepNumber;
    }

    public int lastProcessMapStep() {
        return this.lastProcessMapStep;
    }

    public Object toObject() {
        return this;
    }

    public byte[] toBytes() {
        byte[] cmBytes;

        try {
            cmBytes = ObjectBytes.getBytes(this);

        } catch (Exception ex) {
            cmBytes = null;
            System.out.println(ex.getMessage());
            System.out.println("Could not serialize CozmicMessage");

        }

        return cmBytes;
    }

    public static CozmicMessage fromBytes(byte[] messageBytes) {
        CozmicMessage message;

        try {
            message = (CozmicMessage) ObjectBytes.getObject(messageBytes);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.out.println("Could not convert to CozmicMessage");
            message = null;

        }

        return message;

    }

    public Buffer toBuffer() {
        Buffer cmBuffer;

        try {
            cmBuffer = new Buffer(ObjectBytes.getBytes(this));

        } catch (Exception ex) {
            cmBuffer = null;
            System.out.println(ex.getMessage());
            System.out.println("Could not serialize CozmicMessage");

        }

        return cmBuffer;
    }

    public static CozmicMessage bufferToMessage(Buffer messageBuffer) {
        CozmicMessage message;

        try {
            message = (CozmicMessage) ObjectBytes.getObject(messageBuffer.getBytes());

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.out.println("Could not convert to CozmicMessage");
            message = null;

        }

        return message;

    }


    protected void finalize() throws Throwable {
        data = null;
        processMap = null;
        normalizedData = null;
        currentData = null;
        super.finalize();

    }

}