package com.study.cli.state;

import com.study.facade.RbacFacade;

/**
 * State pattern for menu management with number-based navigation
 */
public interface MenuState {
    /**
     * Display current menu
     */
    void displayMenu(RbacFacade facade);
    
    /**
     * Handle menu choice by number
     * @param input user input (number)
     * @param facade RBAC facade
     * @return next menu state, or null if staying in current state
     */
    MenuState handleMenuChoice(String input, RbacFacade facade);
}
