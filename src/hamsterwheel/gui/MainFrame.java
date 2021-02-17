package hamsterwheel.gui;

import hamsterwheel.core.Controller;
import hamsterwheel.core.MouseUpdate;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MainFrame extends JFrame {

    private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private static MainPanel mainPanel = null;


    public MainFrame() {
        try {
            mainPanel = new MainPanel();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setFocusable(true);

        if (Controller.config.isFullScreen()) setFullscreen();
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

    private void hideCursor() {
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        getContentPane().setCursor(blankCursor);
    }
}
