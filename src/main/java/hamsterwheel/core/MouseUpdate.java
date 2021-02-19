package hamsterwheel.core;

import java.awt.*;
import java.util.Arrays;

public class MouseUpdate {
    private long time;
    private Point position;
    private int dpi, pollingRate;
    private MouseUpdate previous;
    private Integer[] buttonsPressed;


    public MouseUpdate() {
        this.time = System.nanoTime();
        this.position = new Point();
        this.dpi = 1600;
        this.pollingRate = 0;
        this.previous = null;
        this.buttonsPressed = new Integer[0];
    }

    public MouseUpdate(long time, Point position, int dpi, int pollingRate, MouseUpdate previous, Integer[] buttonsPressed) {
        this.time = time;
        this.position = position;
        this.dpi = dpi;
        this.pollingRate = pollingRate;
        this.previous = previous;
        this.buttonsPressed = buttonsPressed;
    }

    @Override
    public String toString() {
        return "%.2f,%s,%s,%s,%s,%s".formatted(getTimeSinceLastUpdate(), position.x, position.y, dpi, pollingRate, Arrays.toString(buttonsPressed));
    }

    public long getTime() {
        return time;
    }

    public Point getPosition() {
        return position;
    }

    public int getDpi() {
        return dpi;
    }

    public int getPollingRate() {
        return pollingRate;
    }

    public MouseUpdate getPrevious() {
        return previous;
    }

    public Integer[] getButtonsPressed() {
        return buttonsPressed;
    }

    public float getTimeSinceLastUpdate() {
        if (this.previous != null) {
            long diffInNanos = this.time - previous.time;
            return diffInNanos/1000000f;
        } else return 0;

    }

    public void setPrevious(MouseUpdate mouseUpdate) {
        this.previous = mouseUpdate;
    }
}
