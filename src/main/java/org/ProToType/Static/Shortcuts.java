package org.ProToType.Static;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Shortcuts {
    public static void PrintWithTime(String stringToPrint) {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd, HH:mm:ss");
        String dateTime = localDateTime.format(dateTimeFormatter);

        System.out.printf("[%s] %s\n", dateTime, stringToPrint);
    }
}
