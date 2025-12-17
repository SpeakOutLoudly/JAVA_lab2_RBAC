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

    public static void close() {
        scanner.close();
    }
}
