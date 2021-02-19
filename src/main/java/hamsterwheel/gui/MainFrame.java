package hamsterwheel.gui;

import hamsterwheel.config.Config;
import hamsterwheel.core.Controller;
import hamsterwheel.core.MouseLocator;
import hamsterwheel.core.MouseUpdate;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class MainFrame extends JFrame {

    private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private static MainPanel mainPanel = null;

    public MainFrame(MouseLocator mouseLocator, MouseListener mouseListener, Config config) {
        try {
            mainPanel = new MainPanel(mouseLocator, mouseListener, config);
        } catch (AWTException e) {
            e.printStackTrace();
        }

        setTitle("HamsterWheel " + Controller.VERSION);

        // call custom exit function after closing JFrame
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Controller.exit();
            }
        });

        // keyboard events won't work without this
        setFocusable(true);

        if (config.isFullScreen()) setFullscreen();
        else setWindowed();

        this.add(mainPanel);
        this.addKeyListener(mainPanel);

        setVisible(true);
    }

    public void handleMouseUpdate(MouseUpdate mouseUpdate) {
        mainPanel.handlePosition(mouseUpdate);
    }

    public void setFullscreen() {
        hideCursor();
        setSize(screenSize.width, screenSize.height);
        setLocation(0, 0);
        setResizable(false);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
    }

    public void setWindowed() {
        setSize(screenSize.width / 2, screenSize.height / 2);
        setLocation(screenSize.width / 4, screenSize.height / 4);
        setResizable(true);
    }

    // replace cursor with a 16x16 transparent image
    private void hideCursor() {
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        getContentPane().setCursor(blankCursor);
    }

    public void addStatsLog(String s) {
        mainPanel.addStatsLog(s);
    }

    public void addDebugLog(String s) {
        mainPanel.addDebugLog(s);
    }
}
