package com.study.repository;

import com.study.exception.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base repository with transaction support
 */
public abstract class BaseRepository {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final DatabaseConnection dbConnection;
    
    public BaseRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }
    
    /**
     * Execute operation within a transaction
     */
    protected <T> T executeInTransaction(TransactionCallback<T> callback) {
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);
            
            T result = callback.doInTransaction(conn);
            
            conn.commit();
            return result;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            throw new PersistenceException("Transaction failed", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Failed to close connection", e);
                }
            }
        }
    }
    
    @FunctionalInterface
    protected interface TransactionCallback<T> {
        T doInTransaction(Connection conn) throws Exception;
    }
}
