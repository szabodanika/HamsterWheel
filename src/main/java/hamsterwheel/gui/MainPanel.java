package hamsterwheel.gui;

import hamsterwheel.config.Config;
import hamsterwheel.core.Controller;
import hamsterwheel.core.MouseLocator;
import hamsterwheel.core.MouseUpdate;
import hamsterwheel.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainPanel extends JPanel implements KeyListener {

    private static final long NANOS_TO_STATIONARY = 1000000000;
    private static final int COORDINATE_BACKLOG_LENGTH = 1000;

    private Config config;
    private MouseLocator mouseLocator;

    private MouseUpdate latestUpdate = new MouseUpdate();
    private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private int lineHeight;

    private Color cursorColor = Color.RED, cursorButtonPressedColor = Color.BLUE, coordinateColor = Color.decode("#db50eb"), coordinateButtonPressedColor = Color.BLUE,
            inchGridColor = Color.decode("#b33d8b"), pixelGridColor = Color.decode("#545fa8"), textColor = Color.BLACK, darkModeTextColor = Color.WHITE;
    private boolean stationary = true;
    private int pollingRate, fps, repaintCounter = 0, maxPollingRate = 0, pollingRateClass = 0, avgPollingRate = 0, longestJump = 0, shortestJump = Integer.MAX_VALUE,
            rgbCycle = 0, currentAcceleration = 0, highestAcceleration = 0, lastJump;
    private long lastTimeMoved = System.nanoTime(), lastTimeStationary = System.nanoTime();

    private List<Integer> last5pollingRates = new ArrayList<Integer>();

    private List<String> statsLogs = Collections.synchronizedList(new ArrayList<>());
    private List<String> debugLogs = Collections.synchronizedList(new ArrayList<>());


    public MainPanel(MouseLocator mouseLocator, MouseListener mouseListener, Config config) throws AWTException {
        setVisible(true);
        this.config = config;
        this.mouseLocator = mouseLocator;
        lineHeight = (int) (4.5 * config.getUIMultiplier());
        this.setFocusable(true);
        addMouseListener(mouseListener);
        startPaintFrequencyCounterThread();
        startPainterThread();
        startRGBThread();
        startStationaryTimer();
    }

    public void handlePosition(MouseUpdate mouseUpdate) {
        latestUpdate = mouseUpdate;
        lastTimeMoved = System.nanoTime();
        if (mouseUpdate.getPrevious() != null)
            calculateJump(mouseUpdate.getPosition(), mouseUpdate.getPrevious().getPosition());
        if (mouseUpdate.getPrevious() != null)
            calculateAcceleration(mouseUpdate.getPosition(), mouseUpdate.getPrevious().getPosition());
        this.pollingRate = mouseUpdate.getPollingRate();
        if (pollingRate > maxPollingRate) maxPollingRate = pollingRate;
        calculateAveragePollingRate(pollingRate);
        calculatePollingRateClass(pollingRate);

    }

    private void startStationaryTimer() {
        new Thread(() -> {
            while (!Thread.interrupted()) {
                if (System.nanoTime() - lastTimeMoved >= NANOS_TO_STATIONARY) {
                    currentAcceleration = 0;
                    stationary = true;
                    lastTimeStationary = System.nanoTime();
                } else {
                    stationary = false;
                }
            }
        }).start();
    }

    private void calculateAcceleration(Point lastPos, Point prevPos) {
        // TODO: need to rework this

        float dx = Math.abs(lastPos.x - prevPos.x);
        float dy = Math.abs(lastPos.y - prevPos.y);
        int currentspeed = pollingRateClass * (int) Math.sqrt(dx * dx + dy * dy);
        float dt = System.nanoTime() - lastTimeStationary;
        // don't bother calculating acceleration for the first 20ms of movement
        if (dt > 20000000) {
            currentAcceleration = (int) (currentspeed / dt * 1000000000);
            // don't check against highest accelaration if it's under 5000pxpss
            if (currentAcceleration > 5000) {
                if (currentAcceleration > highestAcceleration) highestAcceleration = currentAcceleration;
            }
        } else currentAcceleration = 0;
    }

    private void calculateJump(Point lastPos, Point prevPos) {
        float dx = Math.abs(lastPos.x - prevPos.x);
        float dy = Math.abs(lastPos.y - prevPos.y);
        int dist = (int) Math.sqrt(dx * dx + dy * dy);
        lastJump = dist;
        if (dist > longestJump) longestJump = dist;
        if (dist < shortestJump) shortestJump = dist;
    }

    private void calculatePollingRateClass(Integer mouseUpdateFrequency) {
        int pollRateClassLimit = 180;
        int pollRateClass = 125;

        while (true) {
            if (mouseUpdateFrequency < pollRateClassLimit) {
                if (pollRateClass > this.pollingRateClass) this.pollingRateClass = pollRateClass;
                else break;
            } else {
                pollRateClassLimit *= 2;
                pollRateClass *= 2;
            }
        }
    }

    private void calculateAveragePollingRate(Integer mouseUpdateFrequency) {
        last5pollingRates.add(mouseUpdateFrequency);
        if (last5pollingRates.size() > 4) last5pollingRates.remove(0);
        avgPollingRate = 0;
        for (int i = 0; i < last5pollingRates.size(); i++) {
            avgPollingRate += last5pollingRates.get(i);
        }
        avgPollingRate /= last5pollingRates.size();
    }

    private void startPainterThread() {
        new Thread(() -> {
            while (!Thread.interrupted()) {
                repaintCounter++;
                this.repaint();
                try {
                    Thread.sleep(1000 / config.getMaxFPS());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startRGBThread() {
        new Thread(() -> {
            while (!Thread.interrupted()) {
                if (rgbCycle == 99) rgbCycle = 0;
                else rgbCycle++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startPaintFrequencyCounterThread() {
        new Thread(() -> {
            while (!Thread.interrupted()) {
                fps = repaintCounter;
                repaintCounter = 0;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        if (config.isDarkMode()) {
            g2d.setColor(Color.BLACK);
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.fillRect(0, 0, getWidth(), getHeight());
        if (config.isDrawPixelGrid()) paintPixelGrid(g2d);
        if (config.isDrawInchGrid()) paintInchGrid(g2d);
        if (config.isDrawTrail()) paintPath(g2d);
        if (config.isDrawCoordinates()) paintCoordinates(g2d);
        if (config.isDrawRGB()) paintRGB(g2d);
        if (latestUpdate != null) paintCursor(g2d);
        paintUI(g2d);
    }

    public void addDebugLog(String s) {
        this.debugLogs.add(0, s);
        if (config.isShowPollingPanel()) {
            while (debugLogs.size() > (this.getHeight() * 0.25 / lineHeight) - 6 && !debugLogs.isEmpty())
                debugLogs.remove(debugLogs.size() - 1);
        } else {
            while (debugLogs.size() > (this.getHeight() / lineHeight) - 6 && !debugLogs.isEmpty())
                debugLogs.remove(debugLogs.size() - 1);
        }
    }

    public void addStatsLog(String s) {
        this.statsLogs.add(0, s);

        if (config.isShowDebugPanel()) {
            while (statsLogs.size() > (this.getHeight() * 0.75 / lineHeight) - 4 && !statsLogs.isEmpty())
                statsLogs.remove(statsLogs.size() - 1);
        } else {
            while (statsLogs.size() > (this.getHeight() / lineHeight) - 8 && !statsLogs.isEmpty())
                statsLogs.remove(statsLogs.size() - 1);
        }
    }

    private void paintUI(Graphics2D g2d) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("┌── HAMSTER WHEEL   F1 ─────────────────────────────────┐\n");
        if (config.isShowTitlePanel()) {
            stringBuilder.append("│ Version              %8s                         │\n".formatted(Controller.VERSION));
            stringBuilder.append("│                                                       │\n");
            stringBuilder.append("│ Made with love by BitDani                             │\n");
            stringBuilder.append("│ github.com/szabodanika/HamsterWheel                   │\n");
        }
        stringBuilder.append("└───────────────────────────────────────────────────────┘\n");
        stringBuilder.append("\n");
        stringBuilder.append("┌── STATISTICS   F2 ────────────────────────────────────┐\n");
        if (config.isShowStatsPanel()) {
            stringBuilder.append("│ Position             %8d px     %8d px      │\n".formatted(latestUpdate.getPosition().x, latestUpdate.getPosition().y));
            stringBuilder.append("│                                                       │\n");
            stringBuilder.append("│ Polling rate         %8d Hz                      │\n".formatted(pollingRate));
            stringBuilder.append("│ Polling rate MAX     %8d Hz                      │\n".formatted(maxPollingRate));
            stringBuilder.append("│ Polling rate AVG     %8d Hz                      │\n".formatted(avgPollingRate));
            stringBuilder.append("│ Polling rate class   %8d Hz                      │\n".formatted(pollingRateClass));
            stringBuilder.append("│                                                       │\n");
            stringBuilder.append("│ Longest jump dist.   %8d px     %8.4f inch    │\n".formatted(longestJump, (float) longestJump / config.getDpi()));
            stringBuilder.append("│ Shortest jump dist.  %8d px     %8.4f inch    │\n".formatted(shortestJump == Integer.MAX_VALUE ? 0 : shortestJump, (float) (shortestJump == Integer.MAX_VALUE ? 0 : shortestJump) / config.getDpi()));
            stringBuilder.append("│ Movement speed       %8d px/s   %8.4f inch/s  │\n".formatted(lastJump * longestJump, (float) pollingRateClass * lastJump / config.getDpi()));
            stringBuilder.append("│ Fastest movement     %8d px/s   %8.4f inch/s  │\n".formatted(pollingRateClass * longestJump, (float) pollingRateClass * longestJump / config.getDpi()));
            stringBuilder.append("│ Acceleration         %8d px/s2  %8.4f g       │\n".formatted(currentAcceleration, ((float) currentAcceleration / config.getDpi()) * 0.025900792));
            stringBuilder.append("│ Fastest acceleration %8d px/s2  %8.4f g       │\n".formatted(highestAcceleration, ((float) highestAcceleration / config.getDpi() * 0.025900792)));
            stringBuilder.append("│                                                       │\n");
            stringBuilder.append("│ LMB-RMB Latency      %8.2f ms                      │\n".formatted(mouseLocator.getRelativeClickLatency()));
            stringBuilder.append("│ LMB Duration         %8.2f ms                      │\n".formatted(mouseLocator.getClickDuration()));
            stringBuilder.append("│ LMB Interval         %8.2f ms                      │\n".formatted(mouseLocator.getClickInterval()));
            stringBuilder.append("│                                                       │\n");
            stringBuilder.append("│ Graphics FPS         %8d FPS                     │\n".formatted(fps));
        }
        stringBuilder.append("└───────────────────────────────────────────────────────┘\n");
        stringBuilder.append("\n");
        stringBuilder.append("┌── SETTINGS   F3 ──────────────────────────────────────┐\n");
        if (config.isShowSettingsPanel()) {
            stringBuilder.append("│ Pause                       SPACE     %8s        │\n".formatted(mouseLocator.isPaused()));
            stringBuilder.append("│ Draw trail                    T       %8s        │\n".formatted(config.isDrawTrail()));
            stringBuilder.append("│ Draw coordinates              C       %8s        │\n".formatted(config.isDrawCoordinates()));
            stringBuilder.append("│ Polling rate multiplier       M       %8s        │\n".formatted("1/" + config.getPollrateDivisor()));
            stringBuilder.append("│ DPI                           ↑ ↓     %8s        │\n".formatted(config.getDpi()));
            stringBuilder.append("│ FPS limit                     F       %8s        │\n".formatted(config.getMaxFPS()));
            stringBuilder.append("│ Draw  250px grid              P       %8s        │\n".formatted(config.isDrawPixelGrid()));
            stringBuilder.append("│ Draw 1 inch grid              I       %8s        │\n".formatted(config.isDrawInchGrid()));
            stringBuilder.append("│                                                       │\n");
            stringBuilder.append("│ Cycle UI size                 U       %8s        │\n".formatted(config.getUIMultiplier()));
            stringBuilder.append("│ Dark mode                     D       %8s        │\n".formatted(config.isDarkMode()));
            stringBuilder.append("│ RGB                           G       %8s        │\n".formatted(config.isDrawRGB()));
            stringBuilder.append("│ Write poll data in file      F11      %8s        │\n".formatted(config.isEnableStatisticsLogging()));
            stringBuilder.append("│ Fullscreen                   F12      %8s        │\n".formatted(config.isFullScreen()));
            stringBuilder.append("│                                                       │\n");
            stringBuilder.append("│ Hide/show UI                  H                       │\n");
            stringBuilder.append("│ Reset settings               DEL                      │\n");
            stringBuilder.append("│ Reset stats                   R                       │\n");
            stringBuilder.append("│ Exit                          ESC                     │\n");
        }
        stringBuilder.append("└───────────────────────────────────────────────────────┘\n");
        stringBuilder.append("\nRemember to turn off mouse acceleration/precision enhancements");
        stringBuilder.append("\n");

        for (Integer integer : mouseLocator.buttonsPressed) {
            stringBuilder.append("\nButton %d pressed".formatted(integer));
        }

        printText(g2d, 20, 30, config.isDarkMode() ? darkModeTextColor : textColor, stringBuilder.toString());

        stringBuilder = new StringBuilder();
        int lineCounter = 0;
        //TODO horizontal placement of right side when changing ui size
        stringBuilder.append("┌── DEBUG   F4 ────────────────────────────────┐\n");
        if (config.isShowDebugPanel()) {
            for (String debugLog : debugLogs.toArray(new String[0])) {
                if (config.isShowPollingPanel() && lineCounter >= (this.getHeight() * 0.25 / lineHeight) - 6) break;
                else if (lineCounter >= (this.getHeight() / lineHeight) - 6) break;
                while (debugLog.length() > 44) {
                    stringBuilder.append("│ %-44s │\n".formatted(debugLog.substring(0, 44)));
                    lineCounter++;
                    debugLog = debugLog.substring(44);
                }
                stringBuilder.append("│ %-44s │\n".formatted(debugLog));
                lineCounter++;
            }
        }

        stringBuilder.append("└──────────────────────────────────────────────┘\n");
        stringBuilder.append("\n");
        stringBuilder.append("┌── STATS   F5 ────────────────────────────────┐\n");
        if (config.isShowPollingPanel()) {

            for (String statsLog : statsLogs.toArray(new String[0])) {
                stringBuilder.append("│ %-44s │\n".formatted(statsLog));
            }
        }

        stringBuilder.append("└──────────────────────────────────────────────┘\n");
        printText(g2d, this.getWidth() - 360, 30, config.isDarkMode() ? darkModeTextColor : textColor, stringBuilder.toString());

    }

    private void printText(Graphics2D g2d, int x, int y, Color color, String string) {
        lineHeight = (int) (4.5 * config.getUIMultiplier());
        String line = null;
        AttributedString attributedString = null;
        g2d.setColor(color);
        for (int i = 0; i < string.split("\n").length; i++) {
            line = string.split("\n")[i];
            attributedString = new AttributedString(line);

            if (line.trim().length() != 0) {
                if (config.isDarkMode())
                    attributedString.addAttribute(TextAttribute.BACKGROUND, new Color(110, 80, 100, 120));
                else attributedString.addAttribute(TextAttribute.BACKGROUND, new Color(210, 180, 200, 120));
                attributedString.addAttribute(TextAttribute.FONT, new Font("Courier New", Font.PLAIN, 4 * config.getUIMultiplier()));
                if (config.isDrawRGB())
                    attributedString.addAttribute(TextAttribute.FOREGROUND, new Color(Color.HSBtoRGB((float) i / string.split("\n").length + rgbCycle / 100f, 1, 1)));
            }

            g2d.drawString(attributedString.getIterator(), x, y + lineHeight * i);
        }
    }

    private void paintCursor(Graphics2D g2d) {
        g2d.setColor(mouseLocator.buttonsPressed.isEmpty() ? cursorColor : cursorButtonPressedColor);
        // vertical line
        g2d.fillRect(scalePosition(latestUpdate.getPosition()).x, scalePosition(latestUpdate.getPosition()).y - config.getUIMultiplier() * 2, 1, config.getUIMultiplier() * 4 + 1);
        // horizontal line
        g2d.fillRect(scalePosition(latestUpdate.getPosition()).x - config.getUIMultiplier() * 2, scalePosition(latestUpdate.getPosition()).y, config.getUIMultiplier() * 4 + 1, 1);
        // center pixel
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(scalePosition(latestUpdate.getPosition()).x, scalePosition(latestUpdate.getPosition()).y, 1, 1);
        if (stationary) {
            g2d.drawOval(scalePosition(latestUpdate.getPosition()).x - config.getUIMultiplier() * 5, scalePosition(latestUpdate.getPosition()).y - config.getUIMultiplier() * 5, config.getUIMultiplier() * 10, config.getUIMultiplier() * 10);
        }
    }

    private void paintPixelGrid(Graphics2D graphics2D) {
        graphics2D.setColor(pixelGridColor);
        for (int i = 0; i < this.getWidth(); i += 250 * (this.getWidth() / (float) screenSize.width)) {
            graphics2D.drawLine(i, 0, i, this.getHeight());
        }

        for (int i = 0; i < this.getHeight(); i += 250 * (this.getHeight() / (float) screenSize.height)) {
            graphics2D.drawLine(0, i, this.getWidth(), i);
        }
    }

    private void paintInchGrid(Graphics2D graphics2D) {
        graphics2D.setColor(inchGridColor);
        for (int i = 0; i < this.getWidth(); i += config.getDpi() * (this.getWidth() / (float) screenSize.width)) {
            graphics2D.drawLine(i, 0, i, this.getHeight());
        }

        for (int i = 0; i < this.getHeight(); i += config.getDpi() * (this.getHeight() / (float) screenSize.height)) {
            graphics2D.drawLine(0, i, this.getWidth(), i);
        }
    }

    private void paintPath(Graphics2D graphics2D) {
        float dist = 0, colorMultiplier, distx = 0, disty = 0;

        MouseUpdate currentUpdate = latestUpdate;

        for (int i = 0; i < COORDINATE_BACKLOG_LENGTH && currentUpdate != null && currentUpdate.getPrevious() != null; i++) {
            distx = currentUpdate.getPrevious().getPosition().x - currentUpdate.getPosition().x;
            disty = currentUpdate.getPrevious().getPosition().y - currentUpdate.getPosition().y;
            dist = (float) Math.sqrt(Math.pow(distx, 2) + Math.pow(disty, 2));
            colorMultiplier = (dist > longestJump) ? 1 : dist / (float) longestJump;
            graphics2D.setColor(Color.decode("#%02X%02X00".formatted((int) (255 * colorMultiplier), (int) (255 * (1 - colorMultiplier)))));
            graphics2D.drawLine(scalePosition(currentUpdate.getPrevious().getPosition()).x, scalePosition(currentUpdate.getPrevious().getPosition()).y, scalePosition(currentUpdate.getPosition()).x, scalePosition(currentUpdate.getPosition()).y);
            currentUpdate = currentUpdate.getPrevious();
        }
    }

    private void paintCoordinates(Graphics2D graphics2D) {
        graphics2D.setColor(coordinateColor);
        MouseUpdate currentUpdate = latestUpdate;
        for (int i = 0; i < COORDINATE_BACKLOG_LENGTH && currentUpdate != null; i++) {
            if (currentUpdate.getButtonsPressed().length == 0) {
                graphics2D.setColor(coordinateColor);
                graphics2D.drawOval(scalePosition(currentUpdate.getPosition()).x - 1, scalePosition(currentUpdate.getPosition()).y - 1, 1, 1);
            } else {
                graphics2D.setColor(coordinateButtonPressedColor);
                graphics2D.drawOval(scalePosition(currentUpdate.getPosition()).x - 1, scalePosition(currentUpdate.getPosition()).y - 1, 2, 2);
            }
            currentUpdate = currentUpdate.getPrevious();
        }
    }

    private void paintRGB(Graphics2D graphics2D) {
        for (int i = -5; i < getWidth(); i += 5) {
            graphics2D.setColor(new Color(Color.HSBtoRGB((i / (float) getWidth() + rgbCycle / 100f), 1, 1)));
            graphics2D.drawLine(i, 5, i + 5, 10);
            graphics2D.drawLine(i, getHeight() - 5, i + 5, getHeight() - 10);
        }
    }


    @Override
    public void keyTyped(KeyEvent e) {
        // ignore
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // ignore
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE:
                mouseLocator.setPaused(!mouseLocator.isPaused());
                Log.info("paused: %s".formatted(mouseLocator.isPaused()));
                break;
            case KeyEvent.VK_ESCAPE:
                Controller.exit();
                break;
            case KeyEvent.VK_M:
                if (config.getPollrateDivisor() == 16) config.setPollrateDivisor(1);
                else config.setPollrateDivisor(config.getPollrateDivisor() * 2);
                Log.info("changed setting - pollrate divisor: %s".formatted(config.getPollrateDivisor()));
                break;
            case KeyEvent.VK_F:
                if (config.getMaxFPS() == 480) config.setMaxFPS(30);
                else config.setMaxFPS(config.getMaxFPS() * 2);
                Log.info("changed setting - max graphics fps: %s".formatted(config.getMaxFPS()));
                break;
            case KeyEvent.VK_D:
                config.setDarkMode(!config.isDarkMode());
                Log.info("changed setting - dark mode enabled: %s".formatted(config.isDarkMode()));
                break;
            case KeyEvent.VK_C:
                config.setDrawCoordinates(!config.isDrawCoordinates());
                Log.info("changed setting - draw coordinates: %s".formatted(config.isDrawCoordinates()));
                break;
            case KeyEvent.VK_T:
                config.setDrawTrail(!config.isDrawTrail());
                Log.info("changed setting - draw trail: %s".formatted(config.isDrawTrail()));
                break;
            case KeyEvent.VK_U:
                if (config.getUIMultiplier() == 8) config.setUIMultiplier(2);
                else config.setUIMultiplier(config.getUIMultiplier() + 1);
                Log.info("changed setting - ui size multiplier: %s".formatted(config.getUIMultiplier()));
                break;
            case KeyEvent.VK_DELETE:
                config.init();
                Log.info("config reset");
                break;
            case KeyEvent.VK_R:
                resetStats();
                Log.info("session statistics reset");
                break;
            case KeyEvent.VK_I:
                config.setDrawInchGrid(!config.isDrawInchGrid());
                Log.info("changed setting - draw 1 inch grid: %s".formatted(config.isDrawInchGrid()));
                break;
            case KeyEvent.VK_P:
                config.setDrawPixelGrid(!config.isDrawPixelGrid());
                Log.info("changed setting - draw 250 pixel grid: %s".formatted(config.isDrawPixelGrid()));
                break;
            case KeyEvent.VK_UP:
                if (config.getDpi() == 32000) config.setDpi(100);
                else config.setDpi(config.getDpi() + 100);
                Log.info("changed setting - dpi: %s".formatted(config.getDpi()));
                break;
            case KeyEvent.VK_DOWN:
                if (config.getDpi() == 100) config.setDpi(32000);
                else config.setDpi(config.getDpi() - 100);
                Log.info("changed setting - dpi: %s".formatted(config.getDpi()));
                break;
            case KeyEvent.VK_G:
                config.setDrawRGB(!config.isDrawRGB());
                Log.info("changed setting - RGB: %s".formatted(config.isDrawRGB()));
                break;
            case KeyEvent.VK_F1:
                config.setShowTitlePanel(!config.isShowTitlePanel());
                Log.info("changed setting - show title panel: %s".formatted(config.isShowTitlePanel()));
                break;
            case KeyEvent.VK_F2:
                config.setShowStatsPanel(!config.isShowStatsPanel());
                Log.info("changed setting - show statistics panel: %s".formatted(config.isShowStatsPanel()));
                break;
            case KeyEvent.VK_F3:
                config.setShowSettingsPanel(!config.isShowSettingsPanel());
                Log.info("changed setting - show settings panel: %s".formatted(config.isShowSettingsPanel()));
                break;
            case KeyEvent.VK_F4:
                config.setShowDebugPanel(!config.isShowDebugPanel());
                Log.info("changed setting - show debug panel: %s".formatted(config.isShowDebugPanel()));
                break;
            case KeyEvent.VK_F5:
                config.setShowPollingPanel(!config.isShowPollingPanel());
                Log.info("changed setting - show poll data panel: %s".formatted(config.isShowPollingPanel()));
                break;
            case KeyEvent.VK_F11:
                config.setEnableStatisticsLogging(!config.isEnableStatisticsLogging());
                Log.info("changed setting - save poll data on disk: %s".formatted(config.isEnableStatisticsLogging()));
                break;
            case KeyEvent.VK_F12:
                config.setFullScreen(!config.isFullScreen());
                Controller.loadFrame();
                Log.info("changed setting - fullscreen: %s".formatted(config.isFullScreen()));
                break;
        }
    }

    private void resetStats() {
        latestUpdate.setPrevious(null);
        avgPollingRate = 0;
        shortestJump = Integer.MAX_VALUE;
        maxPollingRate = 0;
        pollingRateClass = 0;
        longestJump = 0;
        highestAcceleration = 0;
    }

    private Point scalePosition(Point position) {
        return new Point((int) (position.x * (this.getWidth() / (float) screenSize.width)), (int) (position.y * (this.getHeight() / (float) screenSize.height)));
    }

}
