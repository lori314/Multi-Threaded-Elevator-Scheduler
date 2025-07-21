import com.oocourse.elevator3.PersonRequest;

public class Passenger {
    private final int id;          // 乘客ID
    private String fromFloor; // 起始楼层
    private final String toFloor;  // 目标楼层
    private final int priority;    // 优先级

    public Passenger(PersonRequest request) {
        this.id = request.getPersonId();
        this.fromFloor = request.getFromFloor();
        this.toFloor = request.getToFloor();
        this.priority = request.getPriority();
    }

    // 获取乘客ID
    public int getId() {
        return id;
    }

    // 获取起始楼层
    public int getFromFloor() {
        int sign = 0;
        if (fromFloor.charAt(0) == 'F') {
            sign = 1;
        }
        else if (fromFloor.charAt(0) == 'B') {
            sign = -1;
        }
        int num = Integer.parseInt(fromFloor.substring(1));
        if (sign == -1) {
            num = num - 1;
        }
        return num * sign;
    }

    public void setFromFloor(String fromFloor) {
        this.fromFloor = fromFloor;
    }

    // 获取目标楼层
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

    // 获取优先级
    public int getPriority() {
        return priority;
    }

    public String getDirection() {
        if (getFromFloor() < getToFloor()) {
            return "UP";
        }
        else {
            return "DOWN";
        }
    }

    @Override
    public String toString() {
        return String.format("Passenger[id=%d, from=%s, to=%s, priority=%d]",
                id, fromFloor, toFloor, priority);
    }
}
