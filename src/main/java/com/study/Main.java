package com.study;

import com.study.cli.CliApplication;
import com.study.facade.RbacFacade;
import com.study.repository.DatabaseConnection;

/**
 * Main entry point for RBAC CLI application
 */
public class Main {
    public static void main(String[] args) {
        // Initialize database and facade
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();
        dbConnection.initializeDefaults();
        RbacFacade facade = new RbacFacade(dbConnection);

        // Start CLI application
        CliApplication cliApp = new CliApplication(facade);
        cliApp.start();
    }
}
