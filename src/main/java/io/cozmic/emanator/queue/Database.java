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

package io.cozmic.emanator.queue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Created by Eric on 4/17/2014.
 */
public class Database {
    static final String connectString = "jdbc:h2:~/datastore/emanator_queue";
    static final String queuestore_table_sql = "CREATE TABLE IF NOT EXISTS queue_store (key LONG PRIMARY KEY, value LONGVARBINARY, queue_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP())";
    static String user = "emanator";
    static String password = "emanator";
    static int instances = 0;


    public static void init() {
        Statement stmt = null;
        Connection connection = null;
        instances++;

        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection(connectString, user, password );
            stmt = connection.createStatement();
            stmt.executeUpdate(queuestore_table_sql);
            connection.commit();
            System.out.println("Emanator Queue Datastore Created/Initialized");
        }
        catch( Exception e ) {
            System.out.println( e.getMessage() );
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                if (connection != null)
                    connection.close();
            } catch (Exception ex) {
                //
            }

        }
    }
}
