package javacore.third.util;

import javacore.third.entity.RobotPart;

import java.util.Random;

public class Determinator {
    public static RobotPart determineRobotPartRandomly() {
        Random random = new Random();

        int randomInt = random.nextInt(4) + 1;

        return switch (randomInt) {
            case 1 -> RobotPart.HEAD;
            case 2 -> RobotPart.TORSO;
            case 3 -> RobotPart.HAND;
            default -> RobotPart.FEET;
        };
    }
}
