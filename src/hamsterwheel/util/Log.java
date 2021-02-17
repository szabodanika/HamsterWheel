package hamsterwheel.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    private static Date sessionStart = new Date();
    private static DateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss"),
            logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static boolean statsLogInitialized = false;

    public static void info(Object o) {
        log("info", "[INFO] " + o, true);
    }

    public static void warning(Object o) {
        log("info", "[WARNING] " + o, true);
    }

    public static void error(Object o) {
        log("info", "[ERROR] " + o, true);
    }

    public static void stats(Object o) {
        if (!statsLogInitialized) log("stats", "nanos,x,y,dpi,pollingrate", false);
        log("stats", o, false);
    }

    public static void log(String file, Object o, boolean print) {
        o = "[%s] ".formatted(logDateFormat.format(new Date())) + o.toString();
        if(print) System.out.println(o);
        try {
            IO.writeToFile("logs/"+ file + fileDateFormat.format(sessionStart) + ".log",
                    false, o);
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
