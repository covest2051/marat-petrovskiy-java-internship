package javacore.third;

import javacore.third.entity.Factory;
import javacore.third.entity.Warehouse;
import javacore.third.entity.WednesdayFaction;
import javacore.third.entity.WorldFaction;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int totalDays = 100;
        Warehouse warehouse = new Warehouse();

        Factory factory = new Factory(warehouse, totalDays);
        WorldFaction world = new WorldFaction(warehouse, totalDays);
        WednesdayFaction wednesday = new WednesdayFaction(warehouse, totalDays);

        factory.start();
        world.start();
        wednesday.start();

        factory.join();
        world.join();
        wednesday.join();

        System.out.println("\nResult:");
        System.out.println("World faction total robots count: " + world.getRobotCount());
        System.out.println("Wednesday faction total robots count: " + wednesday.getRobotCount());

        if (world.getRobotCount() > wednesday.getRobotCount()) {
            System.out.println("World!");
        } else if (world.getRobotCount() < wednesday.getRobotCount()) {
            System.out.println("Wednesday!");
        } else {
            System.out.println("Draw");
        }

        System.out.println("World faction inventory remains " + world.getInventory() + ". Can create robot: " + world.canCreateRobot());
        System.out.println("Wednesday faction inventory remains " + wednesday.getInventory() + ". Can create robot: " + wednesday.canCreateRobot());
    }
}
