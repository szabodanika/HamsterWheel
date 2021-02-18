package hamsterwheel.util;

import hamsterwheel.config.Config;

import java.io.IOException;

public class ConfigIO {
    public static void writeConfig(Config config) throws IOException {
        IO.writeToFile("config.cfg", true, config.toString());
    }

    public static Config readConfig(String path) throws IOException {
        return new Config().parse(IO.readFromFile(path));
    }
}
