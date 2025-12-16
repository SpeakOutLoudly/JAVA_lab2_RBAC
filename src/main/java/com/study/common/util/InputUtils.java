package com.study.common.util;

import java.io.Console;
import java.util.Scanner;

/**
 * Utility class for reading user input
 */
public class InputUtils {
    private static final Scanner scanner = new Scanner(System.in);
    
    /**
     * Read a line of input from user
     */
    public static String readInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    /**
     * Read password from user (attempts to hide input if console is available)
     */
    public static String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword(prompt);
            return new String(passwordChars);
        } else {
            // Fallback for IDE environments
            System.out.print(prompt);
            return scanner.nextLine().trim();
        }
    }
    
    /**
     * Close the scanner (call when application exits)
     */
    public static void close() {
        scanner.close();
    }
}
