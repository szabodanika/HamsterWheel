package hamsterwheel.core;

import hamsterwheel.config.Config;
import hamsterwheel.util.ConfigIO;
import hamsterwheel.gui.MainFrame;
import hamsterwheel.util.Log;

import java.io.IOException;

public class Controller {

    public static final String VERSION = "beta 0.4";

    private static Config config;
    private static MainFrame gui;
    private static MouseLocator mouseLocator;

    private static String configFilePath = "config.cfg";

    public static void main(String[] args) {

        boolean start = true;
        for (int i = 0; i < args.length; i++) {
            if(args[i].equals("-v") || args[i].equals("-version")) {
                // print version info
                System.out.println("Hamster Wheel %s made by Daniel Szabo (BitDani)\ngithub.com/szabodanika/HamsterWheel".formatted(VERSION));
                start = false;
            } else if(args[i].equals("-rc") || args[i].equals("-resetconfig")) {
                // reset config file
                //TODO delete config file
            } else if(args[i].equals("-c") || args[i].equals("-config")) {
                // specify config file path
                //TODO delete config file
            } else if(args[i].equals("-d") || args[i].equals("-debug")) {
                // specify debug log file path
                //TODO delete config file
            } else if(args[i].equals("-s") || args[i].equals("-stats")) {
                // specify stat log file path
                //TODO delete config file
            }
        }
        if(!start) return;

        Log.info("Starting HamsterWheel version %s.".formatted(VERSION));
        Log.addLogConsumer(System.out::println);

        loadConfig();
        loadMouseLocator();
        loadFrame();
        mouseLocator.start();

        Log.addStatConsumer(gui::addStatsLog);
        Log.addLogConsumer(gui::addDebugLog);

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
                    gui.handleMouseUpdate(mouseUpdate);
                    Log.stats(mouseUpdate, config.isEnableStatisticsLogging());
                }, config);
        Log.info("Mouse analyser thread started");
    }

    public static void loadFrame() {
        Log.info("Loading GUI...");
        if (gui != null) {
            gui.dispose();
        }
        gui = new MainFrame(mouseLocator, mouseLocator, config);
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

        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }).start();

    }
}
