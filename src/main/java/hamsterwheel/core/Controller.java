package hamsterwheel.core;

import hamsterwheel.config.Config;
import hamsterwheel.util.ConfigIO;
import hamsterwheel.gui.MainFrame;
import hamsterwheel.util.Log;

import java.io.IOException;

public class Controller {

    public static final String VERSION = "beta 0.3";

    public static Config config;
    public static MainFrame mainFrame;
    public static hamsterwheel.core.MouseLocator mouseLocator;

    private static String configFilePath = "config.cfg";

    public static void main(String[] args) {
        Log.info("Starting HamsterWheel version %s.".formatted(VERSION));

        loadConfig();
        loadFrame();
        loadMouseLocator();
    }

    private static void loadConfig() {
        try {
            config = ConfigIO.readConfig(configFilePath);
        } catch (IOException e) {
            Log.warning("Config file %s not found".formatted(configFilePath));
            config = new Config();
            try {
                ConfigIO.writeConfig(config);
            } catch (IOException ioException) {
                ioException.printStackTrace();
                Log.error("Failed to initialize config file %s".formatted(configFilePath));
            }
        }
        Log.info("Config file %s loaded".formatted(configFilePath));
    }

    private static void loadMouseLocator() {
        Log.info("Starting mouse analyser thread...");
        mouseLocator = new MouseLocator(
                mouseUpdate -> {
                    mainFrame.handleMouseUpdate(mouseUpdate);
                    if(config.isEnableStatisticsLogging()) Log.stats(mouseUpdate);
                });
        mouseLocator.start();
        Log.info("Mouse analyser thread started");
    }

    public static void loadFrame() {
        Log.info("Loading GUI...");
        if(mainFrame != null){
            mainFrame.dispose();
        }
        mainFrame = new MainFrame();
        Log.info("GUI loaded");
    }


    public static void exit() {
        try {
            Log.info("Saving config file %s".formatted(configFilePath));
            ConfigIO.writeConfig(config);
        } catch (IOException e) {
            Log.error("Failed to save config file %s".formatted(configFilePath));
            e.printStackTrace();
        }
        Log.info("Goodbye.");
        System.exit(0);
    }
}
