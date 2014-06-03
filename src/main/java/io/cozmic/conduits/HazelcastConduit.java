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

package io.cozmic.conduits;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;


/**
 * Created by Eric on 4/30/2014.
 */
public abstract class HazelcastConduit extends Conduit {
    public String queueName;
    public String clusterName;
    public String clusterPassword;
    public String address;
    protected HazelcastInstance hazelcast;
    protected com.hazelcast.client.config.ClientConfig clientConfig;


    @Override
    public void start() {
        super.start();

        queueName = config.getString("queue_name");
        clusterName = config.getString("cluster_name");
        clusterPassword = config.getString("cluster_password");
        address = config.getString("cluster_address");

        clientConfig = new ClientConfig();

        clientConfig.getGroupConfig().setName(clusterName).setPassword(clusterPassword);
        clientConfig.getNetworkConfig().addAddress(address);

        hazelcast = HazelcastClient.newHazelcastClient(clientConfig);

    }


}
