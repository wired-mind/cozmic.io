package io.cozmic.minders;


import io.cozmic.CozmicComponent;
import io.cozmic.CozmicMessage;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.Server;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.sql.*;

;


/**
 * Created by Eric on 4/17/2014.
 */
public class JournalDatastore extends CozmicComponent {
    static final String journalstore_table_sql = "CREATE TABLE IF NOT EXISTS Journal(messageUUID VARCHAR PRIMARY KEY, lastProcessMapStep INT, " +
            "data LONGVARBINARY, processMap VARCHAR, normalizedData LONGVARBINARY, currentData LONGVARBINARY, " +
            "entry_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP());";
    private String dataDir;
    private String dataStoreName;
    private String serverConnectString;
    private String localConnectString;
    private String tcpPort = "9100";
    private String webPort = "8033";
    private String user;
    private String password;

    private JdbcConnectionPool connectionPool;

    private Server h2Db;
    private Server h2Web;
    static int instances = 0;
    public String owner;


    public void init() {
        String[] tcpArgs = {"-baseDir", dataDir, "-tcpPort", tcpPort, "-tcpAllowOthers", "-ifExists"};
        String[] webArgs = {"-webSSL", "-baseDir", dataDir, "-webPort", webPort, "-webAllowOthers", "-ifExists"};
        localConnectString = "jdbc:h2:" + dataDir + dataStoreName;
        serverConnectString = "jdbc:h2:tcp://localhost:" + tcpPort + "/" + dataDir + dataStoreName;
        logger.error(serverConnectString);
        Statement stmt = null;
        Connection connection = null;
        instances++;

        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection(localConnectString, user, password);
            stmt = connection.createStatement();
            stmt.executeUpdate(journalstore_table_sql);
            connection.commit();
            logger.info("Journal Datastore Created/Initialized");

            try {
                h2Db = Server.createTcpServer(tcpArgs);
                h2Web = Server.createWebServer(webArgs);

            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }

        } catch (Exception ex) {
            logger.error(ex.getMessage());
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                if (connection != null)
                    connection.close();
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }

        }
    }

    public void start() {
        super.start();
        inboundAddress = config.getString("journal_address");
        dataDir = config.getString("base_dir");
        dataStoreName = config.getString("datastore_name");
        user = config.getString("user");
        password = config.getString("password");
        owner = config.getString("owner");

        init();

        try {
            @SuppressWarnings("unchecked")
            Handler<Message<byte[]>> journalMessageHandler = new JournalMessageHandler();
            eb.registerHandler(inboundAddress, journalMessageHandler);

            h2Db.start();
            h2Web.start();
            logger.info("Journal datastore started for: " + owner);
        } catch (Exception ex) {
            logger.error(ex.getMessage());

        }

        try {
            connectionPool = JdbcConnectionPool.create(serverConnectString, user, password);
            Connection database = connectionPool.getConnection();
            database.setAutoCommit(true);
            logger.info("Connected to Journal datastore");

        } catch (Exception ex) {
            logger.error("Could not connect to Journal datastore");
            logger.error(serverConnectString);
            logger.error(ex.getMessage());
        }


    }

    private class JournalMessageHandler implements Handler<Message<byte[]>> {
        public void handle(final Message<byte[]> message) {
            final CozmicMessage cm = CozmicMessage.fromBytes(message.body());
            persist(cm.messageUUID(), cm.lastProcessMapStep(), cm.data(), cm.processMap(), cm.normalizedData(), cm.currentData());

        }
    }

    private void persist(String messageUUID, int lastProcessMapStep, byte[] data, JsonObject processMap, byte[] normalizedData, byte[] currentData) {
        String sql = "INSERT INTO Journal (messageUUID, lastProcessMapStep, data, processMap, normalizedData, currentData) VALUES(?, ?, ?, ?, ?, ?)";
        PreparedStatement statement = null;
        Connection database = null;

        try {
            database = connectionPool.getConnection();
            statement = database.prepareStatement(sql);
            statement.setString(1, messageUUID);
            statement.setInt(2, lastProcessMapStep);
            statement.setBytes(3, data);
            statement.setString(4, processMap.toString());
            statement.setBytes(5, normalizedData);
            statement.setBytes(6, currentData);
            statement.executeUpdate();
            //logger.info("item stored");

        } catch (SQLException ex) {
            logger.error("Could not store journal item");
            logger.error(ex.getMessage());

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
}

