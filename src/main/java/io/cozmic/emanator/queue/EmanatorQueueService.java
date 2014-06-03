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

package io.cozmic.emanator.queue;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import io.cozmic.CozmicComponent;


import java.io.FileNotFoundException;

/**
 * Created by Eric on 5/27/2014.
 */
public class EmanatorQueueService extends CozmicComponent {

    public void start() {


        super.start();

        try {
            Database.init();

            Config hzlConfig = new FileSystemXmlConfig(config.getString("hazelcast_config"));
            final HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(hzlConfig);
            final IQueue<byte[]> distributionQueue = hazelcast.getQueue("Emanator.Queue");
            logger.info("Emanator Queue Started...");

        } catch (FileNotFoundException ex) {
            logger.error(ex.getMessage());
            logger.error("Starting default Hazelcast Instance");
            final HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance();
            final IQueue<byte[]> distributionQueue = hazelcast.getQueue("Emanator.Queue");

        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

    }

}
