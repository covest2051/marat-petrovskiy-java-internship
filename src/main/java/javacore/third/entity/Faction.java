package javacore.third.entity;

import lombok.Getter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// В выводе дней в два раза больше чем ночей потому что у Faction общий счётчик :)
@Getter
public abstract class Faction extends Thread {
    protected final Warehouse warehouse;
    protected final String factionName;
    protected final int MAX_CARRIED_PARTS = 5;

    protected final Map<RobotPart, Integer> inventory = new EnumMap<>(RobotPart.class);

    protected final List<Robot> robots = new ArrayList<>();

    protected final int totalDays;

    public Faction(String factionName, Warehouse warehouse, int totalDays) {
        this.factionName = factionName;
        this.warehouse = warehouse;
        this.totalDays = totalDays;
        setName(factionName + "Thread");

        for (RobotPart part : RobotPart.values()) {
            inventory.put(part, 0);
        }
    }

    @Override
    public void run() {
        for (int day = 1; day <= totalDays; day++) {
            synchronized (warehouse) {
                while (!warehouse.isDayOver()) {
                    if (warehouse.isFinished()) return;

                    try {
                        warehouse.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Faction don't work at night");
                        return;
                    }
                }

                System.out.println("Night " + day + ": " + factionName + " going for the details");

                List<RobotPart> stolenParts = warehouse.takeParts(MAX_CARRIED_PARTS);

                for (RobotPart part : stolenParts) {
                    inventory.put(part, inventory.get(part) + 1);
                }

                System.out.println(factionName + " stole: " + stolenParts);
                createRobots();

                warehouse.setDayOver(false);
                warehouse.notifyAll();
            }
        }
    }

    public void createRobots() {
        while (canCreateRobot()) {
            List<RobotPart> spentParts = List.of(RobotPart.HEAD, RobotPart.TORSO, RobotPart.HAND, RobotPart.HAND, RobotPart.FEET, RobotPart.FEET);

            inventory.put(RobotPart.HEAD, inventory.get(RobotPart.HEAD) - 1);
            inventory.put(RobotPart.TORSO, inventory.get(RobotPart.TORSO) - 1);
            inventory.put(RobotPart.HAND, inventory.get(RobotPart.HAND) - 2);
            inventory.put(RobotPart.FEET, inventory.get(RobotPart.FEET) - 2);

            robots.add(new Robot(spentParts));

            System.out.println(factionName + " created robot. Total robot count: " + robots.size());
        }
    }

    public boolean canCreateRobot() {
        return inventory.get(RobotPart.HEAD) >= 1 &&
                inventory.get(RobotPart.TORSO) >= 1 &&
                inventory.get(RobotPart.HAND) >= 2 &&
                inventory.get(RobotPart.FEET) >= 2;
    }

    public int getRobotCount() {
        return robots.size();
    }
}
