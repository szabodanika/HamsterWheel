import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MainFrame extends JFrame  {

    private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private static MouseCapturePanel mouseCapturePanel = null;

    static {
        try {
            mouseCapturePanel = new MouseCapturePanel();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public MainFrame() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setFocusable(true);


        setFullscreen();
        hideCursor();

        this.add(mouseCapturePanel);
        this.addKeyListener(mouseCapturePanel);

        setVisible(true);
    }

    private void setFullscreen() {
        setSize(screenSize.width, screenSize.height);
        setLocation(0, 0);
        setResizable(false);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
    }

    private void hideCursor() {
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        getContentPane().setCursor(blankCursor);
    }


}
