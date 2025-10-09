package javacore.third.entity;

import java.util.ArrayList;
import java.util.List;

public class Robot {
    private final List<RobotPart> parts = new ArrayList<>();

    public Robot(List<RobotPart> parts) {
        if(isValid(parts)) {
            this.parts.addAll(parts);
        }
    }

    private boolean isValid(List<RobotPart> parts) {
        long heads = parts.stream().filter(robotPart -> robotPart.equals(RobotPart.HEAD)).count();
        long torsos = parts.stream().filter(robotPart -> robotPart.equals(RobotPart.TORSO)).count();
        long hands = parts.stream().filter(robotPart -> robotPart.equals(RobotPart.HAND)).count();
        long feet  = parts.stream().filter(robotPart -> robotPart.equals(RobotPart.FEET)).count();

        return heads >= 1 && torsos >= 1 && hands >= 2 && feet >= 2;
    }

    @Override
    public String toString() {
        return "Robot{" +
                "parts=" + parts +
                '}';
    }
}
