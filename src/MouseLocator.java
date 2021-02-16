import java.awt.*;
import java.util.function.Consumer;

public class MouseLocator extends Thread {

    private Consumer<Point> locationConsumer;
    private Consumer<Integer> pollrateConsumer;
    private Point currPosition = null, prevPosition = null;
    private int pollrateDivisor = 1,  mouseUpdateCounter = 0, dpi = 1600;

    public MouseLocator(Consumer<Point> locationConsumer, Consumer<Integer> pollrateConsumer) {
        this.locationConsumer = locationConsumer;
        this.pollrateConsumer = pollrateConsumer;
    }

    @Override
    public void run() {
        new Thread(() -> {
            while (!Thread.interrupted()) {
                pollrateConsumer.accept(mouseUpdateCounter);
                mouseUpdateCounter = 0;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        int i = 1;
        while (!Thread.interrupted()) {
            currPosition = MouseInfo.getPointerInfo().getLocation();
            if (!currPosition.equals(prevPosition)) {
                prevPosition = currPosition;
                if (i < pollrateDivisor) {
                    i++;
                } else {

                    mouseUpdateCounter++;
                    locationConsumer.accept(currPosition);
                    i = 1;
                }
            }
        }
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
}
