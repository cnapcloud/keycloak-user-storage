package com.keycloak.userstorage.utils;

public class SnakeToCamelConverter {

    public static String toCamelCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean toUpper = false;

        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                toUpper = true;
            } else {
                if (toUpper) {
                    result.append(Character.toUpperCase(c));
                    toUpper = false;
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

}
