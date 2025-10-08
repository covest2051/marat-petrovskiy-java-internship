package javacore.third.entity;

import java.util.ArrayList;
import java.util.List;

public class Warehouse {
    private final List<RobotPart> accessibleParts = new ArrayList<>();
    private boolean dayOver = false;
    private boolean finished = false;

    public void addParts(List<RobotPart> createdParts) {
        accessibleParts.addAll(createdParts);
    }

    public synchronized boolean isDayOver() {
        return dayOver;
    }

    public synchronized void setDayOver(boolean value) {
        this.dayOver = value;
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized void setFinished(boolean finished) {
        this.finished = finished;
    }

    public synchronized List<RobotPart> takeParts(int maxCarriedParts) {
        List<RobotPart> takenParts = new ArrayList<>();

        while (accessibleParts.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return takenParts;
            }
        }

        int countOfTakenParts = 0;

        while (countOfTakenParts < maxCarriedParts && !accessibleParts.isEmpty()) {
            takenParts.add(accessibleParts.removeFirst());
            countOfTakenParts++;
        }

        return takenParts;
    }
}
