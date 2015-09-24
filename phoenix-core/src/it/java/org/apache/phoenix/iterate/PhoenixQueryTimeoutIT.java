package org.apache.phoenix.iterate;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.phoenix.end2end.BaseHBaseManagedTimeIT;
import org.apache.phoenix.jdbc.PhoenixStatement;
import org.junit.Test;

/**
 * Tests to validate that user specified property phoenix.query.timeoutMs
 * works as expected.
 */
public class PhoenixQueryTimeoutIT extends BaseHBaseManagedTimeIT {

    @Test
    /**
     * This test validates that we timeout as expected. It does do by
     * setting the timeout value to 1 ms.
     */
    public void testCustomQueryTimeoutWithVeryLowTimeout() throws Exception {
        // Arrange
        PreparedStatement ps = loadDataAndPrepareQuery(1, 1);
        
        // Act + Assert
        try {
            ResultSet rs = ps.executeQuery();
            // Trigger HBase scans by calling rs.next
            while (rs.next()) {};
            fail("Expected query to timeout with a 1 ms timeout");
        } catch (Exception e) {
            // Expected
        }
    }
    
    @Test
    public void testCustomQueryTimeoutWithNormalTimeout() throws Exception {
        // Arrange
        PreparedStatement ps = loadDataAndPrepareQuery(30000, 30);
        
        // Act + Assert
        try {
            ResultSet rs = ps.executeQuery();
            // Trigger HBase scans by calling rs.next
            int count = 0;
            while (rs.next()) {
                count++;
            }
            assertEquals("Unexpected number of records returned", 1000, count);
        } catch (Exception e) {
            fail("Expected query to suceed");
        }
    }

    
    //-----------------------------------------------------------------
    // Private Helper Methods
    //-----------------------------------------------------------------
    
    private PreparedStatement loadDataAndPrepareQuery(int timeoutMs, int timeoutSecs) throws Exception, SQLException {
        createTableAndInsertRows(1000);
        Properties props = new Properties();
        props.setProperty("phoenix.query.timeoutMs", String.valueOf(timeoutMs));
        Connection conn = DriverManager.getConnection(getUrl(), props);
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM QUERY_TIMEOUT_TEST");
        PhoenixStatement phoenixStmt = ps.unwrap(PhoenixStatement.class);
        assertEquals(timeoutMs, phoenixStmt.getQueryTimeoutInMillis());
        assertEquals(timeoutSecs, phoenixStmt.getQueryTimeout());
        return ps;
    }
    
    private Set<String> createTableAndInsertRows(int numRows) throws Exception {
        String ddl = "CREATE TABLE QUERY_TIMEOUT_TEST (K VARCHAR NOT NULL PRIMARY KEY, V VARCHAR)";
        Connection conn = DriverManager.getConnection(getUrl());
        conn.createStatement().execute(ddl);
        String dml = "UPSERT INTO QUERY_TIMEOUT_TEST VALUES (?, ?)";
        PreparedStatement stmt = conn.prepareStatement(dml);
        final Set<String> expectedKeys = new HashSet<>(numRows);
        for (int i = 1; i <= numRows; i++) {
            String key = "key" + i;
            expectedKeys.add(key);
            stmt.setString(1, key);
            stmt.setString(2, "value" + i);
            stmt.executeUpdate();
        }
        conn.commit();
        return expectedKeys;
    }
}
