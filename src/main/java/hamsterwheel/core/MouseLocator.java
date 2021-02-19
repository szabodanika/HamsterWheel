package hamsterwheel.core;

import hamsterwheel.config.Config;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MouseLocator extends Thread implements MouseListener {

    private Config config;
    private Consumer<MouseUpdate> positionConsumer;
    private Thread pollingRateMeasurerThread;

    private MouseUpdate mouseUpdate = null;
    public List<Integer> buttonsPressed = new ArrayList<>();
    private int mouseUpdateCounter = 0, currentPollingRate = 0;
    private boolean paused = false;

    private long lastRightClickPressed, lastLeftClickPressed, lastLeftClickReleased;
    private float lastClickDuration, lastClickInterval;

    public MouseLocator(Consumer<MouseUpdate> positionConsumer, Config config) {
        this.positionConsumer = positionConsumer;
        this.config = config;
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


        Point currentPosition = null;
        int pollSkipping = 1;
        int pollsBeforeUpdate = 0;
        while (!Thread.interrupted()) {
            while (paused) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            currentPosition = MouseInfo.getPointerInfo().getLocation();
            pollsBeforeUpdate++;
            if (mouseUpdate == null || !currentPosition.equals(mouseUpdate.getPosition())) {
                mouseUpdate = new MouseUpdate(System.nanoTime(), currentPosition,
                        config.getDpi(), currentPollingRate, mouseUpdate, buttonsPressed.toArray(new Integer[0]));
//                if (mouseUpdate.getTimeSinceLastUpdate() > 2000000) {
//                    System.out.println(mouseUpdate.getTimeSinceLastUpdate() / (float) 1000000 + " ms " + pollsBeforeUpdate);
//                }

                pollsBeforeUpdate = 0;
                if (pollSkipping < config.getPollrateDivisor()) {
                    pollSkipping++;
                } else {
                    mouseUpdateCounter++;
                    positionConsumer.accept(mouseUpdate);
                    pollSkipping = 1;
                }
            }
        }
        pollingRateMeasurerThread.interrupt();
    }

    public float getRelativeClickLatency() {
        float latency = (this.lastRightClickPressed - this.lastLeftClickPressed) / 1000000f;
        if(latency > 1000 || latency < -1000) return 0;
        else return latency;
    }

    public float getClickInterval() {
        return this.lastClickInterval;
    }

    public float getClickDuration() {
        return this.lastClickDuration;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == 1) lastLeftClickPressed = System.nanoTime();
        else if (e.getButton() == 3) lastRightClickPressed = System.nanoTime();

        if (lastLeftClickPressed == 0 || lastLeftClickReleased == 0) this.lastClickInterval = 0;
        else this.lastClickInterval = (this.lastLeftClickPressed - this.lastLeftClickReleased) / 1000000f;

        buttonsPressed.add(e.getButton());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == 1) lastLeftClickReleased = System.nanoTime();
        if (lastLeftClickPressed == 0 || lastLeftClickReleased == 0) this.lastClickDuration = 0;
        else this.lastClickDuration = (this.lastLeftClickReleased - this.lastLeftClickPressed) / 1000000f;
        for (int i = 0; i < buttonsPressed.size(); i++) {
            if (buttonsPressed.get(i) == e.getButton()) {
                buttonsPressed.remove(i);
                return;
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
