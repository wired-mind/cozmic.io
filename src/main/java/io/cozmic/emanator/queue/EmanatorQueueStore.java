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


import com.hazelcast.core.QueueStore;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Eric on 4/17/2014.
 */


public class EmanatorQueueStore implements QueueStore<byte[]> {
    static final String connectString = Database.connectString;
    static final String user = "emanator";
    static final String password = "emanator";
    private JdbcConnectionPool connectionPool;


    public EmanatorQueueStore() {
        try {
            connectionPool = JdbcConnectionPool.create(connectString, user, password);
            Connection database = connectionPool.getConnection();
            database.setAutoCommit(true);
            System.out.println("Connected to QueueStore Database");
        } catch (Exception ex) {
            System.out.println("Could not connect to QueueStore");
            System.out.println(ex.getMessage());
        }
    }


    /**
     * Stores the key-value pair.
     *
     * @param key   key of the entry to store
     * @param value value of the entry to store
     */
    @Override
    public void store(Long key,  byte[] value) {
        String sql = "INSERT INTO queue_store (key, value) VALUES(?, ?)";
        PreparedStatement statement = null;
        Connection database = null;

        try {
            database = connectionPool.getConnection();
            statement = database.prepareStatement(sql);
            statement.setLong(1, key);
            statement.setBytes(2, value);
            statement.executeUpdate();
            //System.out.println("item stored");

        } catch (SQLException ex) {
            System.out.println("Could not store queue item");
            System.out.println(ex.getMessage());

        } finally {
            try {
                if (statement != null)
                    statement.close();

                if (database != null)
                    database.close();

            } catch (Exception ex) {
                //
            }
        }
    }


    /**
     * Stores multiple entries. Implementation of this method can optimize the
     * store operation by storing all entries in one database connection for instance.
     *
     * @param map map of entries to store
     */
    @Override
    public void storeAll(Map<Long, byte[]> map) {
        String sql = "INSERT INTO queue_store (key, value) VALUES(?, ?)";
        PreparedStatement statement = null;
        Connection database = null;

        //System.out.println("store all");

        try {
            database = connectionPool.getConnection();
            statement = database.prepareStatement(sql);

            for (Map.Entry<Long, byte[]> entry : map.entrySet()) {
                statement.setLong(1, entry.getKey());
                statement.setBytes(2, entry.getValue());
                statement.addBatch();
            }

            statement.executeBatch();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());

        } finally {
            try {
                if (statement != null)
                    statement.close();

                if (database != null)
                    database.close();

            } catch (Exception ex) {
                //
            }
        }
    }


    /**
     * Deletes the entry with a given key from the store.
     *
     * @param key key to delete from the store.
     */
    public void delete(Long key) {
        String sql = "DELETE FROM queue_store WHERE key = ?";
        PreparedStatement statement = null;
        Connection database = null;

        try {
            database = connectionPool.getConnection();
            statement = database.prepareStatement(sql);
            statement.setLong(1, key);
            statement.executeUpdate();
            //System.out.println("item deleted");

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());

        } finally {
            try {
                if (statement != null)
                    statement.close();

                if (database != null)
                    database.close();

            } catch (Exception ex) {
                //
            }
        }
    }


    /**
     * Deletes multiple entries from the store.
     *
     * @param keys keys of the entries to delete.
     */
    @Override
    public void deleteAll(Collection<Long> keys) {
        String sql = "DELETE FROM queue_store WHERE key = ?";
        PreparedStatement statement = null;
        Connection database = null;

        //System.out.println("store all");

        try {
            database = connectionPool.getConnection();
            statement = database.prepareStatement(sql);

            for (Long key : keys) {
                statement.setLong(1, key);
                statement.addBatch();
            }

            statement.executeBatch();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());

        } finally {
            try {
                if (statement != null)
                    statement.close();

                if (database != null)
                    database.close();

            } catch (Exception ex) {
                //
            }
        }

    }


    /**
     * Loads the value of a given key. If distributed map doesn't contain the value
     * for the given key then Hazelcast will call implementation's load (key) method
     * to obtain the value. Implementation can use any means of loading the given key;
     * such as an O/R mapping tool, simple SQL or reading a file etc.
     *
     * @param key
     * @return value of the key
     */
    @Override
    public byte[] load(Long key) {
        String sql = "SELECT value FROM queue_store WHERE key = ?";
        PreparedStatement statement = null;
        byte[] item = null;
        Connection database = null;

        try {
            database = connectionPool.getConnection();
            statement = database.prepareStatement(sql);

            statement.setLong(1, key);
            ResultSet result = statement.executeQuery();
            result.first();
            item = result.getBytes(1);

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());

        } finally {
            try {
                if (statement != null)
                    statement.close();

                if (database != null)
                    database.close();

            } catch (Exception ex) {
                //
            }
        }
        return item;
    }


    /**
     * Loads given keys. This is batch load operation so that implementation can
     * optimize the multiple loads.
     *
     * @param keys keys of the values entries to load
     * @return map of loaded key-value pairs.
     */
    @Override
    public Map<Long, byte[]> loadAll(Collection<Long> keys) {
        String sql = "SELECT value FROM queue_store WHERE key = ?";
        PreparedStatement statement = null;
        byte[] item = null;
        Connection database = null;
        Map<Long, byte[]> allItems = new HashMap<>();;


        try {
            database = connectionPool.getConnection();
            statement = database.prepareStatement(sql);

            for (long key : keys) {
                try {
                    statement.setLong(1, key);
                    ResultSet result = statement.executeQuery();
                    result.first();
                    allItems.put(key, result.getBytes(1));
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }

        } catch (SQLException ex) {
            System.out.println("SQL Exception- Could not load all Items");

        }

        catch (NullPointerException ex) {
            System.out.println(ex.getMessage());

        } finally {
            try {
                if (statement != null)
                    statement.close();

                if (database != null)
                    database.close();

            } catch (Exception ex) {
                //
            }
        }

        return allItems;
    }


    /**
     * Loads all of the keys from the store.
     *
     * @return all the keys
     */
    @Override
    public Set<Long> loadAllKeys() {
        String sql = "SELECT key FROM queue_store";
        PreparedStatement statement = null;
        Set<Long> allKeys = new HashSet<>();
        Connection database = null;

        //System.out.println("loadAllKeys");

        try {
            database = connectionPool.getConnection();
            statement = database.prepareStatement(sql);

            ResultSet result = statement.executeQuery();
            while (result.next()) {
                //System.out.println("key " + result.getLong("key") + " loaded.");
                allKeys.add(result.getLong(1));
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());

        } finally {
            try {
                if (statement != null)
                    statement.close();

                if (database != null)
                    database.close();

            } catch (Exception ex) {
                //
            }
        }

        return allKeys;
    }
}