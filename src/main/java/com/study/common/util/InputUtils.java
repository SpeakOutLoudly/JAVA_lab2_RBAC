package com.study.common.util;

import java.io.Console;
import java.util.Locale;
import java.util.Scanner;

/**
 * Utility class for reading user input.
 */
public class InputUtils {
    private static final Scanner scanner = new Scanner(System.in);

    private InputUtils() {}

    public static String readInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static long readLong(String prompt) {
        while (true) {
            String input = readInput(prompt);
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid number, please try again.");
            }
        }
    }

    public static int readInt(String prompt) {
        while (true) {
            String input = readInput(prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid number, please try again.");
            }
        }
    }

    public static int readIntOrDefault(String prompt, int defaultValue) {
        while (true) {
            String input = readInput(prompt);
            if (input.isBlank()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid number, please try again.");
            }
        }
    }

    public static <E extends Enum<E>> E readEnum(String prompt, Class<E> enumType) {
        while (true) {
            String input = readInput(prompt);
            try {
                return Enum.valueOf(enumType, input.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                System.out.println("Unknown option, please try again.");
            }
        }
    }

    public static String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword(prompt);
            return new String(passwordChars);
        }
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /**
     * Read email input with validation.
     * Empty input is allowed, non-empty input must match email format.
     * 
     * @param prompt the prompt message
     * @return validated email or empty string
     */
    public static String readEmail(String prompt) {
        while (true) {
            String input = readInput(prompt);
            if (input.isBlank()) {
                return "";
            }
            if (input.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return input;
            }
            System.out.println("Invalid email format. Please enter a valid email or leave blank.");
        }
    }

    /**
     * Read phone input with validation.
     * Empty input is allowed, non-empty input must match phone format.
     * 
     * @param prompt the prompt message
     * @return validated phone or empty string
     */
    public static String readPhone(String prompt) {
        while (true) {
            String input = readInput(prompt);
            if (input.isBlank()) {
                return "";
            }
            if (input.matches("^[0-9+\\-() ]{6,20}$")) {
                return input;
            }
            System.out.println("Invalid phone format. Please enter 6-20 characters (digits, +, -, (), space) or leave blank.");
        }
    }

    public static void close() {
        scanner.close();
    }
}
