package javacore.third.entity;

import javacore.third.util.Determinator;

import java.util.ArrayList;
import java.util.List;

public class Factory extends Thread {
    private final Warehouse warehouse;
    private final int totalDays;

    public Factory(Warehouse warehouse, int totalDays) {
        this.warehouse = warehouse;
        this.totalDays = totalDays;
        setName("FactoryThread");
    }

    @Override
    public void run() {
        for (int day = 1; day <= totalDays; day++) {
            synchronized (warehouse) {
                List<RobotPart> producedParts = produceParts();

                warehouse.addParts(producedParts);
                System.out.println("Day " + day + ": factory produced " + producedParts.size() + " details");

                warehouse.setDayOver(true);
                warehouse.notifyAll();

                while (warehouse.isDayOver()) {
                    try {
                        warehouse.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Factory don't work at night");
                        return;
                    }
                }
            }
        }
        System.out.println("Factory has completed the work");
        synchronized (warehouse) {
            warehouse.setFinished(true);
            warehouse.notifyAll();
        }
    }

    private List<RobotPart> produceParts() {
        List<RobotPart> producedParts = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            RobotPart part = Determinator.determineRobotPartRandomly();
            producedParts.add(part);
        }

        return producedParts;
    }
}
