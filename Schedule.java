import com.oocourse.elevator3.ScheRequest;

public class Schedule {
    private String toFloor;
    private double speed;
    private int id;

    public Schedule(ScheRequest scheRequest) {
        toFloor = scheRequest.getToFloor();
        speed = scheRequest.getSpeed();
        id = scheRequest.getElevatorId();
    }

    public int getId() {
        return id;
    }

    public int getToFloor() {
        int sign = 0;
        if (toFloor.charAt(0) == 'F') {
            sign = 1;
        }
        else if (toFloor.charAt(0) == 'B') {
            sign = -1;
        }
        int num = Integer.parseInt(toFloor.substring(1));
        if (sign == -1) {
            num = num - 1;
        }
        return num * sign;
    }

    public double getSpeed() {
        return speed;
    }
}
