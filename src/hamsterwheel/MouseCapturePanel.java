package hamsterwheel;

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

public class MouseCapturePanel extends JPanel implements KeyListener {

    private static final long NANOS_TO_STATIONARY = 1000000000;

    private MouseLocator mouseLocator;
    private Point position;
    private Color cursorColor = Color.RED,
            pathColor = Color.DARK_GRAY,
            coordinateColor = Color.DARK_GRAY,
            inchGridColor = Color.decode("#b33d8b"),
            pixelGridColor = Color.decode("#545fa8"),
            textColor = Color.WHITE;
    private List<Point> prevPositions = new ArrayList<>();
    private int pollrate, fps, repaintCounter = 0, maxFps = 120, UIMultiplier = 4,
            maxPollingRate = 0, pollingRateClass = 0, avgPollingRate = 0, longestJump = 0, shortestJump = Integer.MAX_VALUE,
            rgbCycle = 0, currentAcceleration = 0, highestAcceleration = 0, lastJump;
    private ArrayList<Integer> last5pollingRates = new ArrayList<>();
    private Robot robot = new Robot();
    private boolean lockCursor = false, darkMode = true, drawTrail = false, drawCoordinates = false, drawInchGrid = false,
            drawPixelGrid = false, showUI = true, drawRGB = false, stationary = true;
    private List<String> messages = new ArrayList<>();
    private long lastTimeMoved = System.nanoTime();
    private long lastTimeStationary = System.nanoTime();
    private List<Integer> buttonsPressed = new ArrayList<>();

    public MouseCapturePanel() throws AWTException {
        this.setFocusable(true);

        setDarkMode(true);
        addMouseListener();
        startPaintFrequencyCounterThread();
        startPainterThread();
        startRGBThread();
        startStationaryTimer();

        mouseLocator = new MouseLocator(
                position -> {
                    this.prevPositions.isEmpty();
                    this.position = position;
                    this.prevPositions.add(this.position);
                    if (prevPositions.size() > 1000) prevPositions.remove(0);
                    if (lockCursor) robot.mouseMove(this.getWidth() / 2, this.getHeight() / 2);
                    lastTimeMoved = System.nanoTime();
                    calculateJump(position);
                    calculateAcceleration(position);
                },
                mouseUpdateFrequency -> {
                    this.pollrate = mouseUpdateFrequency;
                    if (mouseUpdateFrequency > maxPollingRate) maxPollingRate = mouseUpdateFrequency;
                    calculateAveragePollingRate(mouseUpdateFrequency);
                    calculatePollingRateClass(mouseUpdateFrequency);
                });
        mouseLocator.start();
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
                    Thread.sleep(1000 / maxFps);
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
        if (drawTrail) paintPath(g2d);
        if (drawCoordinates) paintCoordinates(g2d);
        if (drawPixelGrid) paintPixelGrid(g2d);
        if (drawInchGrid) paintInchGrid(g2d);
        if (drawRGB) paintRGB(g2d);
        paintCursor(g2d);
        paintStats(g2d);
    }

    private void paintStats(Graphics2D g2d) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("""                 
                ┌─┤ HAMSTER WHEEL ├─────────────────────────────────────┐    
                │ Version              %8s                         │          
                │                                                       │
                │ Made with love by BitDani                             │          
                │ github.com/szabodanika/HamsterWheel                   │                           
                └───────────────────────────────────────────────────────┘  
                                            
                """.formatted(Controller.VERSION));
        stringBuilder.append("┌─┤ STATISTICS ├────────────────────────────────────────┐\n");
        stringBuilder.append("│ Position             %8d px     %8d px      │\n".formatted(position.x, position.y));
        stringBuilder.append("│                                                       │\n");
        stringBuilder.append("│ Polling rate         %8d Hz                      │\n".formatted(pollingRateClass));
        stringBuilder.append("│ Polling rate MAX     %8d Hz                      │\n".formatted(maxPollingRate));
        stringBuilder.append("│ Polling rate AVG     %8d Hz                      │\n".formatted(avgPollingRate));
        stringBuilder.append("│ Polling rate class   %8d Hz                      │\n".formatted(pollingRateClass));
        stringBuilder.append("│                                                       │\n");
        stringBuilder.append("│ Longest jump dist.   %8d px     %8.4f inch    │\n".formatted(longestJump, (float) longestJump / mouseLocator.getDpi()));
        stringBuilder.append("│ Shortest jump dist.  %8d px     %8.4f inch    │\n".formatted(shortestJump == Integer.MAX_VALUE ? 0 : shortestJump, (float) (shortestJump == Integer.MAX_VALUE ? 0 : shortestJump) / mouseLocator.getDpi()));
        stringBuilder.append("│ Movement speed       %8d px/s   %8.4f inch/s  │\n".formatted(lastJump * longestJump, (float) pollingRateClass * lastJump / mouseLocator.getDpi()));
        stringBuilder.append("│ Fastest movement     %8d px/s   %8.4f inch/s  │\n".formatted(pollingRateClass * longestJump, (float) pollingRateClass * longestJump / mouseLocator.getDpi()));
        stringBuilder.append("│ Acceleration         %8d px/s2  %8.4f g       │\n".formatted(currentAcceleration, ((float) currentAcceleration / mouseLocator.getDpi()) * 0.025900792));
        stringBuilder.append("│ Fastest acceleration %8d px/s2  %8.4f g       │\n".formatted(highestAcceleration, ((float) highestAcceleration / mouseLocator.getDpi() * 0.025900792)));
        stringBuilder.append("│                                                       │\n");
        stringBuilder.append("│ Graphics FPS         %8d FPS                     │\n".formatted(fps));
        stringBuilder.append("└───────────────────────────────────────────────────────┘\n");
        stringBuilder.append("\n");
        stringBuilder.append("┌─┤ INSTRUCTIONS ├──────────────────────────────────────┐\n");
        stringBuilder.append("│ Lock cursor                   L       %8s        │\n".formatted(lockCursor));
        stringBuilder.append("│ Dark mode                     D       %8s        │\n".formatted(darkMode));
        stringBuilder.append("│ FPS limit                     F       %8s        │\n".formatted(maxFps));
        stringBuilder.append("│ Polling rate multiplier       M       %8s        │\n".formatted("1/" + mouseLocator.getPollrateDivisor()));
        stringBuilder.append("│ Draw trail                    T       %8s        │\n".formatted(drawTrail));
        stringBuilder.append("│ Draw coordinates              C       %8s        │\n".formatted(drawCoordinates));
        stringBuilder.append("│ Cycle UI size                 U       %8s        │\n".formatted(UIMultiplier));
        stringBuilder.append("│ DPI                           ↑ ↓     %8s        │\n".formatted(mouseLocator.getDpi()));
        stringBuilder.append("│ Draw  250px grid              P       %8s        │\n".formatted(drawPixelGrid));
        stringBuilder.append("│ Draw 1 inch grid              I       %8s        │\n".formatted(drawInchGrid));
        stringBuilder.append("│ RGB                           G       %8s        │\n".formatted(drawRGB));
        stringBuilder.append("│                                                       │\n");
        stringBuilder.append("│ Hide/show UI                  H                       │\n");
        stringBuilder.append("│ Reset settings                S                       │\n");
        stringBuilder.append("│ Reset stats                   R                       │\n");
        stringBuilder.append("│ Exit                          ESC                     │\n");
        stringBuilder.append("└───────────────────────────────────────────────────────┘\n");
        stringBuilder.append("\nRemember to turn off mouse acceleration/precision enhancements");
        stringBuilder.append("\n");
        if (lockCursor) stringBuilder.append("\nUnlock cursor for accurate poll rate readings\n");
        if (stationary) stringBuilder.append("\nStationary\n");

        for (int i = 0; i < buttonsPressed.size(); i++) {
            stringBuilder.append("\nButton %d pressed".formatted(buttonsPressed.get(i)));
        }

        if (showUI) {
            printText(g2d, 50, 50, textColor, stringBuilder.toString());
        } else {
            printText(g2d, 50, 50, textColor, "Hide/show UI   H");
        }

    }

    private void printText(Graphics2D g2d, int x, int y, Color color, String string) {
        int lineHeight = (int) (4.5 * UIMultiplier);
        String line = null;
        AttributedString attributedString = null;
        g2d.setColor(color);
        for (int i = 0; i < string.split("\n").length; i++) {
            line = string.split("\n")[i];
            attributedString = new AttributedString(line);

            if (line.trim().length() != 0) {
                attributedString.addAttribute(TextAttribute.BACKGROUND, new Color(127, 127, 127, 255));
                attributedString.addAttribute(TextAttribute.FONT, new Font("Courier New", Font.PLAIN, 4 * UIMultiplier));
                if (drawRGB)
                    attributedString.addAttribute(TextAttribute.FOREGROUND, new Color(Color.HSBtoRGB((float) i / string.split("\n").length + rgbCycle / 100f, 1, 1)));
            }

            g2d.drawString(attributedString.getIterator(), x, y + lineHeight * i);
        }
    }

    private void paintCursor(Graphics2D g2d) {
        g2d.setColor(cursorColor);
        g2d.fillRect(position.x, position.y - UIMultiplier, 1, UIMultiplier * 2 + 1);
        g2d.fillRect(position.x - UIMultiplier, position.y, UIMultiplier * 2 + 1, 1);
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(position.x, position.y, 1, 1);
    }

    private void paintPixelGrid(Graphics2D graphics2D) {
        graphics2D.setColor(pixelGridColor);
        for (int i = 0; i < this.getWidth(); i += 250) {
            graphics2D.drawLine(i, 0, i, this.getHeight());
        }

        for (int i = 0; i < this.getHeight(); i += 250) {
            graphics2D.drawLine(0, i, this.getWidth(), i);
        }
    }

    private void paintInchGrid(Graphics2D graphics2D) {
        graphics2D.setColor(inchGridColor);
        for (int i = 0; i < this.getWidth(); i += mouseLocator.getDpi()) {
            graphics2D.drawLine(i, 0, i, this.getHeight());
        }

        for (int i = 0; i < this.getHeight(); i += mouseLocator.getDpi()) {
            graphics2D.drawLine(0, i, this.getWidth(), i);
        }
    }

    private void paintPath(Graphics2D graphics2D) {
        graphics2D.setColor(pathColor);
        for (int i = 1; i < prevPositions.size(); i++) {
            graphics2D.drawLine(prevPositions.get(i - 1).x, prevPositions.get(i - 1).y, prevPositions.get(i).x, prevPositions.get(i).y);
        }
    }

    private void paintCoordinates(Graphics2D graphics2D) {
        graphics2D.setColor(Color.GREEN);
        for (int i = 1; i < prevPositions.size(); i++) {
            graphics2D.drawOval(prevPositions.get(i).x - 1, prevPositions.get(i).y - 1, 1, 1);
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
            case KeyEvent.VK_L:
                lockCursor = !lockCursor;
                break;
            case KeyEvent.VK_ESCAPE:
                System.exit(0);
                break;
            case KeyEvent.VK_M:
                if (mouseLocator.getPollrateDivisor() == 16) mouseLocator.setPollrateDivisor(1);
                else mouseLocator.setPollrateDivisor(mouseLocator.getPollrateDivisor() * 2);
                break;
            case KeyEvent.VK_F:
                if (maxFps == 480) maxFps = 30;
                else maxFps *= 2;
                break;
            case KeyEvent.VK_D:
                setDarkMode(!darkMode);
                break;
            case KeyEvent.VK_C:
                drawCoordinates = !drawCoordinates;
                break;
            case KeyEvent.VK_T:
                drawTrail = !drawTrail;
                break;
            case KeyEvent.VK_U:
                if (UIMultiplier == 8) UIMultiplier = 2;
                else UIMultiplier += 1;
                break;
            case KeyEvent.VK_S:
                UIMultiplier = 4;
                mouseLocator.setPollrateDivisor(1);
                setDarkMode(true);
                maxFps = 120;
                drawTrail = false;
                lockCursor = false;
                drawCoordinates = false;
                drawInchGrid = false;
                drawPixelGrid = false;
                break;
            case KeyEvent.VK_R:
                avgPollingRate = 0;
                shortestJump = Integer.MAX_VALUE;
                maxPollingRate = 0;
                pollingRateClass = 0;
                pollrate = 0;
                longestJump = 0;
                highestAcceleration = 0;
                break;
            case KeyEvent.VK_I:
                drawInchGrid = !drawInchGrid;
                break;
            case KeyEvent.VK_P:
                drawPixelGrid = !drawPixelGrid;
                break;
            case KeyEvent.VK_UP:
                if (mouseLocator.getDpi() == 32000) mouseLocator.setDpi(100);
                else mouseLocator.setDpi(mouseLocator.getDpi() + 100);
                break;
            case KeyEvent.VK_DOWN:
                if (mouseLocator.getDpi() == 100) mouseLocator.setDpi(32000);
                else mouseLocator.setDpi(mouseLocator.getDpi() - 100);
                break;
            case KeyEvent.VK_H:
                showUI = !showUI;
                break;
            case KeyEvent.VK_G:
                drawRGB = !drawRGB;
                break;
        }
    }

    private void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        if (darkMode) {
            this.setBackground(Color.BLACK);
            this.textColor = Color.WHITE;
        } else {
            this.setBackground(Color.WHITE);
            this.textColor = Color.BLACK;
        }
    }
}
