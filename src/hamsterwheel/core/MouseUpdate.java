package hamsterwheel.core;

import java.awt.*;

public class MouseUpdate {
    private long time;
    private Point position;
    private int dpi, pollingRate;
    private MouseUpdate previous;

    public MouseUpdate(long time, Point position, int dpi, int pollingRate, MouseUpdate previous) {
        this.time = time;
        this.position = position;
        this.dpi = dpi;
        this.pollingRate = pollingRate;
        this.previous = previous;
    }

    @Override
    public String toString() {
        return "%s,%s,%s,%s,%s".formatted(time, position.getX(), position.getY(), dpi, pollingRate);
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
}
