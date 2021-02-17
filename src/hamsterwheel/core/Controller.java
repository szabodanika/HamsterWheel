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
    public static MouseLocator mouseLocator;

    private static String configFilePath = "config.cfg";

    public static void main(String[] args) {
        Log.info("Starting HamsterWheel version %s.".formatted(VERSION));

        for (int i = 0; i < args.length; i++) {
            if(args[i].equals("-v")) {
                System.out.println(VERSION);
                return;
            }
            if(args[i].equals("-c")) {
                configFilePath = args[i+1];
                return;
            }
        }
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
        mouseLocator = new MouseLocator(
                mouseUpdate -> {
                    mainFrame.handleMouseUpdate(mouseUpdate);
                    if(config.isEnableStatisticsLogging()) Log.stats(mouseUpdate);
                });
        mouseLocator.start();
        Log.info("Mouse analyser thread started");
    }

    public static void loadFrame() {
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
