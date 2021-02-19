package hamsterwheel.util;

import hamsterwheel.core.MouseUpdate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class Log {
    private static Date sessionStart = new Date();
    private static DateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss"),
            logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static boolean statsLogInitialized = false;
    private static List<Consumer<String>> logConsumers = new ArrayList<>(), statConsumers = new ArrayList<>();

    public static void addLogConsumer(Consumer<String> logConsumer) {
        logConsumers.add(logConsumer);
    }

    public static void addStatConsumer(Consumer<String> statConsumer) {
        statConsumers.add(statConsumer);
    }

    public static void removeLogConsumer(Consumer<String> logConsumer) {
        logConsumers.remove(logConsumer);
    }

    public static void removeStatConsumer(Consumer<String> statConsumer) {
        statConsumers.remove(statConsumer);
    }

    public static void info(Object o) {
        log("info", "[INFO] " + o, true, true);
    }

    public static void warning(Object o) {
        log("info", "[WARNING] " + o, true, true);
    }

    public static void error(Object o) {
        log("info", "[ERROR] " + o, true, true);
    }

    public static void stats(MouseUpdate mouseUpdate, boolean writeToFile) {
        if (!statsLogInitialized) {
            log("stats", "nanos,x,y,pollingrate,buttonspressed", false, writeToFile);
            statsLogInitialized = true;
        }
        log("stats", mouseUpdate, false, writeToFile);
    }

    public static void log(String file, Object o, boolean log, boolean writeToFile) {
        String s = String.valueOf(o);
        if (log) {
            s = "[%s] ".formatted(logDateFormat.format(new Date())) + s;
            for (Consumer<String> logConsumer : logConsumers) logConsumer.accept(s);
        } else for (Consumer<String> statConsumer : statConsumers) {
            statConsumer.accept(s);
        }

        if (writeToFile) {
            try {
                IO.writeToFile("logs/" + file + fileDateFormat.format(sessionStart) + ".log", false, s);
            } catch (IOException e) {
                try {
                    Files.createDirectories(Path.of("logs/"));
                    info("Created logs folder for log files");
                } catch (IOException ioException) {
                    System.out.println("Failed to create /logs folder for log files");
                }
            }
        }

    }
}
