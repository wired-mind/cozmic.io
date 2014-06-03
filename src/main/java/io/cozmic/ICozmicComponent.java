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

import org.vertx.java.core.json.JsonObject;


import java.util.Map;

/**
 * Created by Eric on 5/8/2014.
 */
public interface ICozmicComponent {

    public void onConfigUpdate();

    public void updateConfig(JsonObject config);

    public String getComponentId();

    public void setComponentId(String channelId);

    public String retrieveComponentId();
}
