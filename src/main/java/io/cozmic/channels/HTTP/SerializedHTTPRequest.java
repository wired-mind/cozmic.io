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


import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by Eric on 4/28/2014.
 */
public class SerializedHTTPRequest implements Serializable {
        private String version;
        private String method;
        private String uri;
        private String path;
        private String query;
        private List<Map.Entry<String, String>> headers;
        private List<Map.Entry<String, String>> params;


        SerializedHTTPRequest() {}

        public String version() {
            return this.version;
        }

        public void version(String version) {
            this.version = version;
        }

        public String method() {
            return this.method;
        }

        public void method(String method) {
            this.method = method;
        }

        public String uri() {
            return this.uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        public String path() {
            return this.path;
        }

        public void path(String path) {
            this.path = path;
        }

        public String query() {
            return this.query;
        }

        public void query(String query) {
            this.query = query;
        }

        public List<Map.Entry<String, String>> headers() {
            return this.headers;
        }

        public void headers(List<Map.Entry<String, String>> headers) {
            this.headers = headers;
        }

        public List<Map.Entry<String, String>> params() {
            return this.params;
        }

        public void params(List<Map.Entry<String, String>> params) {
            this.params = params;

        }


}

