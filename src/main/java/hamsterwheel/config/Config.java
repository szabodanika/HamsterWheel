package hamsterwheel.config;

import hamsterwheel.util.Log;

import java.lang.reflect.Field;

public class Config {
    private boolean fullScreen, darkMode, drawTrail,
            drawCoordinates, drawInchGrid, drawPixelGrid, drawRGB, enableStatisticsLogging,
            showTitlePanel, showStatsPanel, showSettingsPanel, showDebugPanel, showPollingPanel;
    private int maxFPS, UIMultiplier, pollrateDivisor, dpi;

    public Config() {
        init();
    }

    public Config parse(String string) {
        Config config = new Config();
        String[] lines = string.split("\n");
        String key, value;
        for (String line : lines) {
            key = line.split("=")[0];
            value = line.split("=")[1];
            try {
                Field field = Config.class.getDeclaredField(key);
                switch (Config.class.getDeclaredField(key).getType().getTypeName()) {
                    case "boolean" -> field.set(config, Boolean.parseBoolean(value));
                    case "String" -> field.set(config, value);
                    case "int" -> field.set(config, Integer.parseInt(value));
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                Log.error(e.getMessage());
            }
        }
        return config;
    }

    @Override
    public String toString() {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            for (Field field : this.getClass().getDeclaredFields()) {
                stringBuilder.append(field.getName() + "=" + field.get(this) + "\n");
            }
            return stringBuilder.toString();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public void init() {
        UIMultiplier = 3;
        darkMode = true;
        maxFPS = 120;
        drawTrail = false;
        drawCoordinates = false;
        drawInchGrid = false;
        drawPixelGrid = false;
        pollrateDivisor = 1;
        dpi = 1600;
        enableStatisticsLogging = false;
        showTitlePanel = true;
        showStatsPanel = true;
        showSettingsPanel = true;
        showDebugPanel = false;
        showPollingPanel = false;
        drawRGB = false;
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public void setFullScreen(boolean fullScreen) {
        this.fullScreen = fullScreen;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public boolean isDrawTrail() {
        return drawTrail;
    }

    public void setDrawTrail(boolean drawTrail) {
        this.drawTrail = drawTrail;
    }

    public boolean isDrawCoordinates() {
        return drawCoordinates;
    }

    public void setDrawCoordinates(boolean drawCoordinates) {
        this.drawCoordinates = drawCoordinates;
    }

    public boolean isDrawInchGrid() {
        return drawInchGrid;
    }

    public void setDrawInchGrid(boolean drawInchGrid) {
        this.drawInchGrid = drawInchGrid;
    }

    public boolean isDrawPixelGrid() {
        return drawPixelGrid;
    }

    public void setDrawPixelGrid(boolean drawPixelGrid) {
        this.drawPixelGrid = drawPixelGrid;
    }

    public boolean isShowTitlePanel() {
        return showTitlePanel;
    }

    public void setShowTitlePanel(boolean showTitlePanel) {
        this.showTitlePanel = showTitlePanel;
    }

    public boolean isShowStatsPanel() {
        return showStatsPanel;
    }

    public void setShowStatsPanel(boolean showStatsPanel) {
        this.showStatsPanel = showStatsPanel;
    }

    public boolean isShowSettingsPanel() {
        return showSettingsPanel;
    }

    public void setShowSettingsPanel(boolean showSettingsPanel) {
        this.showSettingsPanel = showSettingsPanel;
    }

    public boolean isShowDebugPanel() {
        return showDebugPanel;
    }

    public void setShowDebugPanel(boolean showDebugPanel) {
        this.showDebugPanel = showDebugPanel;
    }

    public boolean isShowPollingPanel() {
        return showPollingPanel;
    }

    public void setShowPollingPanel(boolean showPollingPanel) {
        this.showPollingPanel = showPollingPanel;
    }

    public boolean isDrawRGB() {
        return drawRGB;
    }

    public void setDrawRGB(boolean drawRGB) {
        this.drawRGB = drawRGB;
    }

    public int getMaxFPS() {
        return maxFPS;
    }

    public void setMaxFPS(int maxFPS) {
        this.maxFPS = maxFPS;
    }

    public int getUIMultiplier() {
        return UIMultiplier;
    }

    public void setUIMultiplier(int UIMultiplier) {
        this.UIMultiplier = UIMultiplier;
    }

    public int getPollrateDivisor() {
        return pollrateDivisor;
    }

    public void setPollrateDivisor(int pollrateDivisor) {
        this.pollrateDivisor = pollrateDivisor;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public boolean isEnableStatisticsLogging() {
        return enableStatisticsLogging;
    }

    public void setEnableStatisticsLogging(boolean enableStatisticsLogging) {
        this.enableStatisticsLogging = enableStatisticsLogging;
    }

}
