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

package io.cozmic.channels.HTTP;



import org.json.JSONException;
import org.json.JSONObject;
import org.vertx.java.core.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by Eric on 4/28/2014.
 */
public class HTTPRequestSerializer {


    public static SerializedHTTPRequest serialize(HttpServerRequest request)  {
        SerializedHTTPRequest serializedRequest = new SerializedHTTPRequest();

        try{
            serializedRequest.version(request.version().toString());
            serializedRequest.method(request.method());
            serializedRequest.uri(request.uri());
            serializedRequest.path(request.path());
            serializedRequest.query(request.query());
            serializedRequest.headers(convertToMultiMap(request.headers().entries()));
            serializedRequest.params(convertToMultiMap(request.params().entries()));


        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }



        return serializedRequest;

    }


    public static JSONObject jsonize (HttpServerRequest request)  {
        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put("version", request.version().toString());
            requestJSON.put("method", request.method());
            requestJSON.put("uri", request.uri());
            requestJSON.put("path", request.path());
            requestJSON.put("query", request.query());
            requestJSON.put("headers", convertToMultiMap(request.headers().entries()));
            requestJSON.put("params", convertToMultiMap(request.params().entries()));

        } catch (JSONException ex) {
            System.out.print(ex.getMessage());

        }

        return requestJSON;

    }

    public static List<Map.Entry<String, String>> convertToMultiMap(List<Map.Entry<String, String>> collection) {
        List<Map.Entry<String, String>> multiMap = new ArrayList<>();

        for (Map.Entry<String, String> entry: collection) {
            multiMap.addAll(collection);
        }

        return multiMap;

    }




}

