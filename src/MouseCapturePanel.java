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
            rgbCycle = 0;
    private ArrayList<Integer> last5pollingRates = new ArrayList<>();
    private Robot robot = new Robot();
    private boolean lockCursor = false, darkMode = true, drawTrail = false, drawCoordinates = false, drawInchGrid = false, drawPixelGrid = false, showUI = true, drawRGB = false;
    private List<String> messages = new ArrayList<>();


    public MouseCapturePanel() throws AWTException {
        this.setFocusable(true);

        setDarkMode(true);
        addMouseListener();
        startPaintFrequencyCounterThread();
        startPainterThread();
        startRGBThread();

        mouseLocator = new MouseLocator(
                position -> {
                    this.prevPositions.isEmpty();
                    this.position = position;
                    this.prevPositions.add(this.position);
                    if (prevPositions.size() > 1000) prevPositions.remove(0);
                    if (lockCursor) robot.mouseMove(this.getWidth() / 2, this.getHeight() / 2);
                    calculateJump(position);
                },
                mouseUpdateFrequency -> {
                    this.pollrate = mouseUpdateFrequency;
                    if (mouseUpdateFrequency > maxPollingRate) maxPollingRate = mouseUpdateFrequency;
                    calculateAveragePollingRate(mouseUpdateFrequency);
                    calculatePollingRateClass(mouseUpdateFrequency);
                });
        mouseLocator.start();
    }

    private void calculateJump(Point position) {
        if (prevPositions.size() > 2) {
            Point lastPosition = this.prevPositions.get(this.prevPositions.size() - 2);
            float dx = Math.abs(position.x - lastPosition.x);
            float dy = Math.abs(position.y - lastPosition.y);
            int dist = (int) Math.sqrt(dx * dx + dy * dy);
            if (dist > longestJump) {
                longestJump = dist;
            }
            if (dist < shortestJump) {
                shortestJump = dist;
            }
        }
    }

    private void calculatePollingRateClass(Integer mouseUpdateFrequency) {
        int pollRateClassLimit = 180;
        int pollRateClass = 125;

        while (true) {
            if (mouseUpdateFrequency < pollRateClassLimit) {
                if (pollRateClass > this.pollingRateClass) {
                    this.pollingRateClass = pollRateClass;
                }
                break;
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
                if(rgbCycle == 99) rgbCycle = 0;
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
                cursorColor = Color.BLUE;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                cursorColor = Color.RED;
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
        if (showUI) {
            printText(g2d, 50, 50, textColor,
                    """                 
                            ┌─┤ HAMSTER WHEEL ├─────────────────────────────────────┐    
                            │ Version              %8s                         │          
                            │                                                       │
                            │ Made with love by BitDani                             │          
                            │ github.com/szabodanika/HamsterWheel                   │                           
                            └───────────────────────────────────────────────────────┘  
                                              
                            ┌─┤ STATISTICS ├────────────────────────────────────────┐                                           
                            │ Position             %8d px     %8d px      │                               
                            │                                                       │                     
                            │ Polling rate         %8d Hz                      │                           
                            │ Polling rate MAX     %8d Hz                      │                           
                            │ Polling rate AVG     %8d Hz                      │                           
                            │ Polling rate class   %8d Hz                      │                           
                            │                                                       │                           
                            │ Longest jump dist.   %8d px     %8.4f inch    │                           
                            │ Shortest jump dist.  %8d px     %8.4f inch    │                           
                            │ Fastest movement     %8d px/s   %8.4f inch/s  │                           
                            │ Fastest acc.         %8d px/s2  %8.4f g       │                           
                            │                                                       │                  
                            │ Graphics FPS         %8d FPS                     │                           
                            └───────────────────────────────────────────────────────┘    
                                            
                            ┌─┤ INSTRUCTIONS ├──────────────────────────────────────┐                                           
                            │ Lock cursor                   L       %8s        │                           
                            │ Dark mode                     D       %8s        │                           
                            │ FPS limit                     F       %8s        │                               
                            │ Polling rate multiplier       M       %8s        │                               
                            │ Draw trail                    T       %8s        │                           
                            │ Draw coordinates              C       %8s        │                           
                            │ Cycle UI size                 U       %8s        │                           
                            │ DPI                           ↑ ↓     %8s        │                           
                            │ Draw  250px grid              P       %8s        │                           
                            │ Draw 1 inch grid              I       %8s        │  
                            │ RGB                           G       %8s        │                           
                            │                                                       │                         
                            │ Hide/show UI                  H                       │                           
                            │ Reset settings                S                       │                           
                            │ Reset stats                   R                       │                           
                            │ Exit                          ESC                     │    
                            └───────────────────────────────────────────────────────┘
                                                    
                            %s
                            """.formatted(
                            Controller.VERSION,
                            position.x,
                            position.y,
                            pollrate,
                            maxPollingRate,
                            avgPollingRate,
                            pollingRateClass,
                            longestJump,
                            (float) longestJump / (float) mouseLocator.getDpi(),
                            shortestJump == Integer.MAX_VALUE ? 0 : shortestJump,
                            (float) (shortestJump == Integer.MAX_VALUE ? 0 : shortestJump) / (float) mouseLocator.getDpi(),
                            pollingRateClass * longestJump,
                            pollingRateClass * longestJump / (float) mouseLocator.getDpi(),
                            0,
                            0f,
                            fps,
                            lockCursor,
                            darkMode,
                            maxFps,
                            "1/" + mouseLocator.getPollrateDivisor(),
                            drawTrail,
                            drawCoordinates,
                            UIMultiplier,
                            mouseLocator.getDpi(),
                            drawPixelGrid,
                            drawInchGrid,
                            drawRGB,
                            lockCursor ? "Unlock cursor for accurate poll rate readings" : ""));
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
                if(drawRGB) attributedString.addAttribute(TextAttribute.FOREGROUND, new Color(Color.HSBtoRGB((float)i/string.split("\n").length + rgbCycle/100f, 1, 1)));
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
            graphics2D.setColor(new Color(Color.HSBtoRGB((i/(float)getWidth() + rgbCycle/100f), 1, 1)));
            graphics2D.drawLine(i, 5, i + 5, 10);
            graphics2D.drawLine(i, getHeight()-5, i + 5, getHeight()-10);
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
