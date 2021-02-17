package hamsterwheel.gui;

import hamsterwheel.core.Controller;
import hamsterwheel.core.MouseUpdate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

public class MainPanel extends JPanel implements KeyListener {

    private static final long NANOS_TO_STATIONARY = 1000000000;

    private Point position = new Point();
    private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    private Color cursorColor = Color.RED,cursorButtonPressedColor = Color.BLUE, coordinateColor = Color.GREEN,
            inchGridColor = Color.decode("#b33d8b"), pixelGridColor = Color.decode("#545fa8"), textColor = Color.WHITE;
    private boolean stationary = true;
    private int pollingRate, fps, repaintCounter = 0, maxPollingRate = 0, pollingRateClass = 0, avgPollingRate = 0, longestJump = 0, shortestJump = Integer.MAX_VALUE,
            rgbCycle = 0, currentAcceleration = 0, highestAcceleration = 0, lastJump;
    private long lastTimeMoved = System.nanoTime(), lastTimeStationary = System.nanoTime();

    private ArrayList<Integer> last5pollingRates = new ArrayList<>();
    private List<Point> prevPositions = new ArrayList<>();
    private List<Integer> buttonsPressed = new ArrayList<>();

    public MainPanel() throws AWTException {
        this.setFocusable(true);
        addMouseListener();
        startPaintFrequencyCounterThread();
        startPainterThread();
        startRGBThread();
        startStationaryTimer();
    }

    public void handlePosition(MouseUpdate mouseUpdate) {
        this.position = mouseUpdate.getPosition();

        this.prevPositions.add(this.position);
        if (prevPositions.size() > 1000) prevPositions.remove(0);
        lastTimeMoved = System.nanoTime();
        calculateJump(position);
        calculateAcceleration(position);

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

    private void calculateAcceleration(Point position) {
        // TODO: need to rework this

        if (prevPositions.size() > 2) {
            Point lastPosition = this.prevPositions.get(this.prevPositions.size() - 2);
            float dx = Math.abs(position.x - lastPosition.x);
            float dy = Math.abs(position.y - lastPosition.y);
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
    }

    private void calculateJump(Point position) {
        if (prevPositions.size() > 2) {
            Point lastPosition = this.prevPositions.get(this.prevPositions.size() - 2);
            float dx = Math.abs(position.x - lastPosition.x);
            float dy = Math.abs(position.y - lastPosition.y);
            int dist = (int) Math.sqrt(dx * dx + dy * dy);
            lastJump = dist;
            if (dist > longestJump) longestJump = dist;
            if (dist < shortestJump) shortestJump = dist;
        }
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
                    Thread.sleep(1000 / Controller.config.getMaxFPS());
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

    private void addMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                buttonsPressed.add(e.getButton());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                for (int i = 0; i < buttonsPressed.size(); i++) {
                    if (buttonsPressed.get(i) == e.getButton()) {
                        buttonsPressed.remove(i);
                        return;
                    }
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        if (Controller.config.isDarkMode()) {
            this.setBackground(Color.BLACK);
        } else {
            this.setBackground(Color.WHITE);
        }

        if (Controller.config.isDrawPixelGrid()) paintPixelGrid(g2d);
        if (Controller.config.isDrawInchGrid()) paintInchGrid(g2d);
        if (Controller.config.isDrawTrail()) paintPath(g2d);
        if (Controller.config.isDrawCoordinates()) paintCoordinates(g2d);
        if (Controller.config.isDrawRGB()) paintRGB(g2d);
        paintCursor(g2d);
        paintStats(g2d);
    }

    private void paintStats(Graphics2D g2d) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("""                 
                ┌── HAMSTER WHEEL ──────────────────────────────────────┐    
                │ Version              %8s                         │          
                │                                                       │
                │ Made with love by BitDani                             │          
                │ github.com/szabodanika/HamsterWheel                   │                           
                └───────────────────────────────────────────────────────┘  
                                            
                """.formatted(Controller.VERSION));
        stringBuilder.append("┌── STATISTICS ─────────────────────────────────────────┐\n");
        stringBuilder.append("│ Position             %8d px     %8d px      │\n".formatted(position.x, position.y));
        stringBuilder.append("│                                                       │\n");
        stringBuilder.append("│ Polling rate         %8d Hz                      │\n".formatted(pollingRate));
        stringBuilder.append("│ Polling rate MAX     %8d Hz                      │\n".formatted(maxPollingRate));
        stringBuilder.append("│ Polling rate AVG     %8d Hz                      │\n".formatted(avgPollingRate));
        stringBuilder.append("│ Polling rate class   %8d Hz                      │\n".formatted(pollingRateClass));
        stringBuilder.append("│                                                       │\n");
        stringBuilder.append("│ Longest jump dist.   %8d px     %8.4f inch    │\n".formatted(longestJump, (float) longestJump / Controller.config.getDpi()));
        stringBuilder.append("│ Shortest jump dist.  %8d px     %8.4f inch    │\n".formatted(shortestJump == Integer.MAX_VALUE ? 0 : shortestJump, (float) (shortestJump == Integer.MAX_VALUE ? 0 : shortestJump) / Controller.config.getDpi()));
        stringBuilder.append("│ Movement speed       %8d px/s   %8.4f inch/s  │\n".formatted(lastJump * longestJump, (float) pollingRateClass * lastJump / Controller.config.getDpi()));
        stringBuilder.append("│ Fastest movement     %8d px/s   %8.4f inch/s  │\n".formatted(pollingRateClass * longestJump, (float) pollingRateClass * longestJump / Controller.config.getDpi()));
        stringBuilder.append("│ Acceleration         %8d px/s2  %8.4f g       │\n".formatted(currentAcceleration, ((float) currentAcceleration / Controller.config.getDpi()) * 0.025900792));
        stringBuilder.append("│ Fastest acceleration %8d px/s2  %8.4f g       │\n".formatted(highestAcceleration, ((float) highestAcceleration / Controller.config.getDpi() * 0.025900792)));
        stringBuilder.append("│                                                       │\n");
        stringBuilder.append("│ Graphics FPS         %8d FPS                     │\n".formatted(fps));
        stringBuilder.append("└───────────────────────────────────────────────────────┘\n");
        stringBuilder.append("\n");
        stringBuilder.append("┌── INSTRUCTIONS ───────────────────────────────────────┐\n");
        stringBuilder.append("│ Pause                       SPACE     %8s        │\n".formatted(Controller.mouseLocator.isPaused()));
        stringBuilder.append("│ Draw trail                    T       %8s        │\n".formatted(Controller.config.isDrawTrail()));
        stringBuilder.append("│ Draw coordinates              C       %8s        │\n".formatted(Controller.config.isDrawCoordinates()));
        stringBuilder.append("│ Polling rate multiplier       M       %8s        │\n".formatted("1/" + Controller.config.getPollrateDivisor()));
        stringBuilder.append("│ DPI                           ↑ ↓     %8s        │\n".formatted(Controller.config.getDpi()));
        stringBuilder.append("│ Dark mode                     D       %8s        │\n".formatted(Controller.config.isDarkMode()));
        stringBuilder.append("│ FPS limit                     F       %8s        │\n".formatted(Controller.config.getMaxFPS()));
        stringBuilder.append("│ Cycle UI size                 U       %8s        │\n".formatted(Controller.config.getUIMultiplier()));
        stringBuilder.append("│ Draw  250px grid              P       %8s        │\n".formatted(Controller.config.isDrawPixelGrid()));
        stringBuilder.append("│ Draw 1 inch grid              I       %8s        │\n".formatted(Controller.config.isDrawPixelGrid()));
        stringBuilder.append("│ RGB                           G       %8s        │\n".formatted(Controller.config.isDrawRGB()));
        stringBuilder.append("│ Statistics logging           F11      %8s        │\n".formatted(Controller.config.isEnableStatisticsLogging()));
        stringBuilder.append("│ Fullscreen                   F12      %8s        │\n".formatted(Controller.config.isFullScreen()));
        stringBuilder.append("│                                                       │\n");
        stringBuilder.append("│ Hide/show UI                  H                       │\n");
        stringBuilder.append("│ Reset settings                S                       │\n");
        stringBuilder.append("│ Reset stats                   R                       │\n");
        stringBuilder.append("│ Exit                          ESC                     │\n");
        stringBuilder.append("└───────────────────────────────────────────────────────┘\n");
        stringBuilder.append("\nRemember to turn off mouse acceleration/precision enhancements");
        stringBuilder.append("\n");
        if (stationary) stringBuilder.append("\nStationary\n");

        for (Integer integer : buttonsPressed) {
            stringBuilder.append("\nButton %d pressed".formatted(integer));
        }

        if (Controller.config.isShowUI()) {
            printText(g2d, 20, 30, textColor, stringBuilder.toString());
        } else {
            printText(g2d, 20, 30, textColor, "Hide/show UI   H");
        }

    }

    private void printText(Graphics2D g2d, int x, int y, Color color, String string) {
        int lineHeight = (int) (4.5 * Controller.config.getUIMultiplier());
        String line = null;
        AttributedString attributedString = null;
        g2d.setColor(color);
        for (int i = 0; i < string.split("\n").length; i++) {
            line = string.split("\n")[i];
            attributedString = new AttributedString(line);

            if (line.trim().length() != 0) {
                attributedString.addAttribute(TextAttribute.BACKGROUND, new Color(127, 127, 127, 255));
                attributedString.addAttribute(TextAttribute.FONT, new Font("Courier New", Font.PLAIN, 4 * Controller.config.getUIMultiplier()));
                if (Controller.config.isDrawRGB())
                    attributedString.addAttribute(TextAttribute.FOREGROUND, new Color(Color.HSBtoRGB((float) i / string.split("\n").length + rgbCycle / 100f, 1, 1)));
            }

            g2d.drawString(attributedString.getIterator(), x, y + lineHeight * i);
        }
    }

    private void paintCursor(Graphics2D g2d) {
        g2d.setColor(buttonsPressed.isEmpty()?cursorColor:cursorButtonPressedColor);
        g2d.fillRect(scalePosition(position).x, scalePosition(position).y - Controller.config.getUIMultiplier(), 1, Controller.config.getUIMultiplier() * 2 + 1);
        g2d.fillRect(scalePosition(position).x - Controller.config.getUIMultiplier(), scalePosition(position).y, Controller.config.getUIMultiplier() * 2 + 1, 1);
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(scalePosition(position).x, scalePosition(position).y, 1, 1);
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
        for (int i = 0; i < this.getWidth(); i += Controller.config.getDpi() * (this.getWidth() / (float) screenSize.width)) {
            graphics2D.drawLine(i, 0, i, this.getHeight());
        }

        for (int i = 0; i < this.getHeight(); i += Controller.config.getDpi() * (this.getHeight() / (float) screenSize.height)) {
            graphics2D.drawLine(0, i, this.getWidth(), i);
        }
    }

    private void paintPath(Graphics2D graphics2D) {
        float dist = 0, colorMultiplier, distx = 0, disty = 0;
        for (int i = 1; i < prevPositions.size()-1; i++) {
            distx = prevPositions.get(i - 1).x - prevPositions.get(i).x;
            disty = prevPositions.get(i - 1).y - prevPositions.get(i).y;
            dist = (float) Math.sqrt(Math.pow(distx,2) + Math.pow(disty, 2));
            colorMultiplier = (dist> longestJump)?1:dist/(float) longestJump;
            graphics2D.setColor(Color.decode("#%02X%02X00".formatted((int) (255*colorMultiplier), (int) (255*(1-colorMultiplier)))));
            graphics2D.drawLine(scalePosition(prevPositions.get(i - 1)).x, scalePosition(prevPositions.get(i - 1)).y, scalePosition(prevPositions.get(i)).x, scalePosition(prevPositions.get(i)).y);
        }
    }

    private void paintCoordinates(Graphics2D graphics2D) {
        graphics2D.setColor(coordinateColor);
        for (int i = 1; i < prevPositions.size(); i++) {
            graphics2D.drawOval(scalePosition(prevPositions.get(i)).x - 1, scalePosition(prevPositions.get(i)).y - 1, 1, 1);
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
                Controller.mouseLocator.setPaused(!Controller.mouseLocator.isPaused());
                break;
            case KeyEvent.VK_ESCAPE:
                Controller.exit();
                break;
            case KeyEvent.VK_M:
                if (Controller.config.getPollrateDivisor() == 16) Controller.config.setPollrateDivisor(1);
                else Controller.config.setPollrateDivisor(Controller.config.getPollrateDivisor() * 2);
                break;
            case KeyEvent.VK_F:
                if (Controller.config.getMaxFPS() == 480) Controller.config.setMaxFPS(30);
                else Controller.config.setMaxFPS(Controller.config.getMaxFPS() * 2);
                break;
            case KeyEvent.VK_D:
                Controller.config.setDarkMode(!Controller.config.isDarkMode());
                break;
            case KeyEvent.VK_C:
                Controller.config.setDrawCoordinates(!Controller.config.isDrawCoordinates());
                break;
            case KeyEvent.VK_T:
                Controller.config.setDrawTrail(!Controller.config.isDrawTrail());
                break;
            case KeyEvent.VK_U:
                if (Controller.config.getUIMultiplier() == 8) Controller.config.setUIMultiplier(2);
                else Controller.config.setUIMultiplier(Controller.config.getUIMultiplier() + 1);
                break;
            case KeyEvent.VK_S:
                Controller.config.init();
                break;
            case KeyEvent.VK_R:
                resetStats();
                break;
            case KeyEvent.VK_I:
                Controller.config.setDrawInchGrid(!Controller.config.isDrawInchGrid());
                break;
            case KeyEvent.VK_P:
                Controller.config.setDrawPixelGrid(!Controller.config.isDrawPixelGrid());
                break;
            case KeyEvent.VK_UP:
                if (Controller.config.getDpi() == 32000) Controller.config.setDpi(100);
                else Controller.config.setDpi(Controller.config.getDpi() + 100);
                break;
            case KeyEvent.VK_DOWN:
                if (Controller.config.getDpi() == 100) Controller.config.setDpi(32000);
                else Controller.config.setDpi(Controller.config.getDpi() - 100);
                break;
            case KeyEvent.VK_H:
                Controller.config.setShowUI(!Controller.config.isShowUI());
                break;
            case KeyEvent.VK_G:
                Controller.config.setDrawRGB(!Controller.config.isDrawRGB());
                break;
            case KeyEvent.VK_F11:
                Controller.config.setEnableStatisticsLogging(!Controller.config.isEnableStatisticsLogging());
                break;
            case KeyEvent.VK_F12:
                Controller.config.setFullScreen(!Controller.config.isFullScreen());
                Controller.loadFrame();
                break;
        }
    }

    private void resetStats() {
        avgPollingRate = 0;
        shortestJump = Integer.MAX_VALUE;
        maxPollingRate = 0;
        pollingRateClass = 0;
        longestJump = 0;
        highestAcceleration = 0;
    }

    private Point scalePosition(Point position) {
        return new Point((int) ( position.x * (this.getWidth() / (float) screenSize.width)), (int) (position.y * ( this.getHeight()/ (float)screenSize.height)));
    }

}
