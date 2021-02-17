package hamsterwheel.core;

import javax.swing.plaf.TableHeaderUI;
import java.awt.*;
import java.util.function.Consumer;

public class MouseLocator extends Thread {

    private Consumer<MouseUpdate> positionConsumer;
    private int mouseUpdateCounter = 0, currentPollingRate = 0;
    private MouseUpdate mouseUpdate = null;
    private boolean paused = false;
    private Thread pollingRateMeasurerThread;

    public MouseLocator(Consumer<MouseUpdate> positionConsumer) {
        this.positionConsumer = positionConsumer;
    }

    @Override
    public void run() {
        pollingRateMeasurerThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                currentPollingRate = mouseUpdateCounter;
                mouseUpdateCounter = 0;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "PollingRateMeasurer");
        pollingRateMeasurerThread.start();

        int i = 1;
        while (!Thread.interrupted()) {
            while (paused) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (mouseUpdate == null || !MouseInfo.getPointerInfo().getLocation().equals(mouseUpdate.getPosition())) {
                mouseUpdate = new MouseUpdate(System.nanoTime(), MouseInfo.getPointerInfo().getLocation(), Controller.config.getDpi(), currentPollingRate, mouseUpdate);
                if (i < Controller.config.getPollrateDivisor()) {
                    i++;
                } else {
                    mouseUpdateCounter++;
                    positionConsumer.accept(mouseUpdate);
                    i = 1;
                }
            }
        }
        pollingRateMeasurerThread.interrupt();
    }

    public Consumer<MouseUpdate> getPositionConsumer() {
        return positionConsumer;
    }

    public void setPositionConsumer(Consumer<MouseUpdate> positionConsumer) {
        this.positionConsumer = positionConsumer;
    }

    public int getMouseUpdateCounter() {
        return mouseUpdateCounter;
    }

    public void setMouseUpdateCounter(int mouseUpdateCounter) {
        this.mouseUpdateCounter = mouseUpdateCounter;
    }

    public int getCurrentPollingRate() {
        return currentPollingRate;
    }

    public void setCurrentPollingRate(int currentPollingRate) {
        this.currentPollingRate = currentPollingRate;
    }

    public MouseUpdate getMouseUpdate() {
        return mouseUpdate;
    }

    public void setMouseUpdate(MouseUpdate mouseUpdate) {
        this.mouseUpdate = mouseUpdate;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }
}
