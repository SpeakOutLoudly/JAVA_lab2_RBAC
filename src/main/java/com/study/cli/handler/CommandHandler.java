package com.study.cli.handler;

import com.study.facade.RbacFacade;

/**
 * Interface for command handlers
 */
public interface CommandHandler {
    void handle(String command, RbacFacade facade);
}
