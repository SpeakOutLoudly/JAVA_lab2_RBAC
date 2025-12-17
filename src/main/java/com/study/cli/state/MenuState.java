package com.study.cli.state;

import com.study.facade.RbacFacade;

/**
 * State pattern for menu management
 */
public interface MenuState {
    void displayMenu(RbacFacade facade);
    void handleCommand(String command, RbacFacade facade);
}
