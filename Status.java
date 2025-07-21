public class Status {
    private int currentFloor;
    private int targetFurtherFloor;
    private int currentLoad;
    private int floorUp;
    private int floorDown;

    public Status() {
        this.currentFloor = 1;
        this.targetFurtherFloor = 1;
        this.currentLoad = 0;
        this.floorUp = 7;
        this.floorDown = -3;
    }

    public void updateStatus(int currentFloor, int targetFurtherFloor, int currentLoad) {
        this.currentFloor = currentFloor;
        this.targetFurtherFloor = targetFurtherFloor;
        this.currentLoad = currentLoad;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public int getTargetFurtherFloor() {
        return targetFurtherFloor;
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public boolean isWait() {
        return currentFloor == targetFurtherFloor && currentLoad == 0;
    }

    public void setFloorUp(int floorUp) {
        this.floorUp = floorUp;
    }

    public void setFloorDown(int floorDown) {
        this.floorDown = floorDown;
    }

    public boolean inRange(int fromFloor, int toFloor) {
        if (fromFloor > floorUp || fromFloor < floorDown) {
            return false;
        }
        return toFloor <= floorUp && toFloor >= floorDown;
    }

    public String getDirection() {
        if (isWait()) {
            return "IDLE";
        }
        else {
            if (currentFloor < targetFurtherFloor) {
                return "UP";
            }
            else if (currentFloor > targetFurtherFloor) {
                return "DOWN";
            }
        }
        return "UNKNOWN";
    }

    public boolean sendAble(Passenger passenger) {
        if (floorUp == 7 && floorDown == -3) {
            return false;
        }
        if (passenger.getFromFloor() > floorUp || passenger.getFromFloor() < floorDown) {
            return false;
        }
        if (passenger.getToFloor() <= floorUp && passenger.getToFloor() >= floorDown) {
            return false;
        }
        if (floorUp != 7 && passenger.getFromFloor() == floorUp) {
            return false;
        }
        if (floorDown != -3 && passenger.getFromFloor() == floorDown) {
            return false;
        }
        return true;
    }

    public void addPassenger() {
        currentLoad++;
    }

    public String toString() {
        return String.format("currentFloor:%d,currentLoad:%d,targetFloor:%d",
                currentFloor, currentLoad, targetFurtherFloor);
    }

    public boolean isUpdated() {
        return floorUp != 7 || floorDown != -3;
    }
}
