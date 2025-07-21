import java.util.ArrayList;
import com.oocourse.elevator3.TimableOutput;

public class Elevator implements Runnable {
    private final PassengerQueue passengers;
    private final ArrayList<Passenger> passengersIn;
    private final ArrayList<Passenger> passengersOut;
    private final ArrayList<Passenger> passengersBack;
    private final ArrayList<Integer> targetFloors;
    private final ScheduleQueue schedules;
    private final UpdateQueue updates;
    private Strategy strategy;
    private int id;
    private double speed;
    private int currentFloor;
    private int currentLoad;
    private boolean isEnd;
    private Status status;
    private double openTime;
    private boolean isScheduled;
    private final Object lock;
    private final Object bigLock;
    private Object smallLock;
    private boolean isUpdate;
    private boolean readyUpdate;
    private boolean doorOpened;
    private boolean hasBack;
    private String type;
    private Elevator friend;
    private int floorUp;
    private int floorDown;

    public Elevator(PassengerQueue passengers,ScheduleQueue schedules,
        UpdateQueue updates,int id,Object bigLock) {
        this.passengers = passengers;
        this.passengersIn = new ArrayList<>();
        this.passengersOut = new ArrayList<>();
        this.passengersBack = new ArrayList<>();
        this.targetFloors = new ArrayList<>();
        this.id = id;
        this.speed = 0.4;
        this.currentFloor = 1;
        this.currentLoad = 0;
        this.floorUp = 7;
        this.floorDown = -3;
        this.isEnd = false;
        this.status = new Status();
        this.openTime = 0.4;
        this.schedules = schedules;
        this.updates = updates;
        this.lock = passengers.getLock();
        this.bigLock = bigLock;
        this.isUpdate = false;
        this.isScheduled = false;
        this.doorOpened = false;
        this.hasBack = false;
        this.type = "FULL";
        this.readyUpdate = false;
        this.strategy = new Strategy();
    }

    @Override
    public void run() {
        while (true) {
            if (isEnd && passengers.isEmpty() && passengersIn.isEmpty() && targetFloors.isEmpty()
                && passengersOut.isEmpty() && schedules.isEmpty() && updates.isEmpty()) {
                break;
            }
            while (targetFloors.isEmpty() && passengers.isEmpty()
                    && schedules.isEmpty() && !isEnd && updates.isEmpty()) {
                synchronized (lock) {
                    try {
                        lock.wait();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                processSchedule();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                processUpdate();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            processPassengers();
            try {
                move();
                processPassengers();
                arrive();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processPassengers() {
        while (!passengers.isEmpty()) {
            Passenger passenger = passengers.poll();
            addTargetFloor(passenger.getFromFloor());
            currentLoad++;
            if (currentLoad == 1 && targetFloors.get(0) == currentFloor) {
                addTargetFloor(passenger.getToFloor());
            }
            passengersOut.add(passenger);
            status.updateStatus(currentFloor,getFurtherFloor(),currentLoad);
        }
    }

    public int getFurtherFloor() {
        return strategy.getFurtherFloor(targetFloors,currentFloor);
    }

    private boolean isConflict(int nextFloor) {
        return strategy.isConflict(nextFloor,friend,type,floorDown,floorUp);
    }

    private void addTargetFloor(int floor) {
        int n = floor;
        if (floor > floorUp) {
            n = floorUp;
            hasBack = true;
        }
        if (floor < floorDown) {
            n = floorDown;
            hasBack = true;
        }
        if (targetFloors.contains(n)) {
            targetFloors.remove((Integer) n);
        }
        targetFloors.add(n);
    }

    private void move() throws InterruptedException {
        if (type.equals("A") || type.equals("B")) {
            if (targetFloors.contains(currentFloor)) { return; }
            int nextFloor = 114514;
            if (currentFloor > getFurtherFloor()) { nextFloor = currentFloor - 1; }
            else if (currentFloor < getFurtherFloor()) { nextFloor = currentFloor + 1; }
            else { return; }
            boolean enteringCriticalZoneA = (nextFloor == floorDown || currentFloor == floorDown);
            boolean enteringCriticalZoneB = (nextFloor == floorUp || currentFloor == floorUp);
            if (enteringCriticalZoneA && type.equals("A") ||
                enteringCriticalZoneB && type.equals("B"))
            {
                synchronized (smallLock) {
                    if (friend.getIsUpdate()) { return; }
                    while (isConflict(nextFloor)) {
                        smallLock.wait();
                    }
                    Thread.sleep((long) (speed * 1000));
                    currentFloor = nextFloor;
                    TimableOutput.println(String.format("ARRIVE-%s-%d",translate(currentFloor),id));
                    status.updateStatus(currentFloor, getFurtherFloor(), currentLoad);
                    smallLock.notifyAll();
                }
            } else {
                Thread.sleep((long) (speed * 1000));
                currentFloor = nextFloor;
                TimableOutput.println(String.format("ARRIVE-%s-%d", translate(currentFloor), id));
                synchronized (smallLock) {
                    status.updateStatus(currentFloor, getFurtherFloor(), currentLoad);
                    smallLock.notifyAll();
                }
            }
        } else {
            if (targetFloors.isEmpty() || targetFloors.contains(currentFloor)) { return; }
            int nextFloor = -999;
            if (currentFloor > getFurtherFloor()) { nextFloor = currentFloor - 1; }
            else if (currentFloor < getFurtherFloor()) { nextFloor = currentFloor + 1; }
            Thread.sleep((long) (speed * 1000));
            currentFloor = nextFloor;
            TimableOutput.println(String.format("ARRIVE-%s-%d", translate(currentFloor), id));
            status.updateStatus(currentFloor,getFurtherFloor(),currentLoad);
        }
    }

    private void arrive() throws InterruptedException {
        ArrayList<Passenger> tempPassengersOut = new ArrayList<>();
        ArrayList<Passenger> tempPassengersIn = new ArrayList<>();
        ArrayList<Passenger> tempPassengersBack = new ArrayList<>();
        Integer temp = 114514;
        for (int targetFloor : targetFloors) {
            if (targetFloor == currentFloor) {
                for (Passenger passenger : passengersIn) {
                    if (passenger.getToFloor() == targetFloor) {
                        tempPassengersIn.add(passenger);
                    }
                }
                for (Passenger passenger : passengersIn) {
                    if (passenger.getToFloor() > floorUp && currentFloor == floorUp) {
                        tempPassengersBack.add(passenger);
                    }
                    else if (passenger.getToFloor() < floorDown && currentFloor == floorDown) {
                        tempPassengersBack.add(passenger);
                    }
                }
                for (Passenger passenger : passengersOut) {
                    if (passenger.getFromFloor() == targetFloor) {
                        tempPassengersOut.add(passenger);
                    }
                }
                temp = targetFloor;
                break;
            }
        }
        boolean needOpen = !(tempPassengersBack.isEmpty() &&
            tempPassengersIn.isEmpty() && tempPassengersOut.isEmpty());
        if (needOpen) {
            openDoor();
            processPassengers();
            for (Passenger passenger : passengersOut) {
                if (passenger.getFromFloor() == currentFloor
                    && !tempPassengersOut.contains(passenger)) {
                    tempPassengersOut.add(passenger);
                }
            }
            processNeedOpen(tempPassengersIn,tempPassengersBack,tempPassengersOut,temp);
            if (targetFloors.isEmpty() && (currentFloor == floorDown && type.equals("A"))) {
                addTargetFloor(floorDown + 1);
            }
            if (targetFloors.isEmpty() && (currentFloor == floorUp && type.equals("B"))) {
                addTargetFloor(floorUp - 1);
            }
            status.updateStatus(currentFloor,getFurtherFloor(),currentLoad);
            if (smallLock != null) {
                synchronized (smallLock) { smallLock.notifyAll(); }
            }
            synchronized (bigLock) {
                bigLock.notify();
            }
        }
        if (temp != 114514 && !needOpen) { targetFloors.remove(temp); }
    }

    private String translate(int floor) {
        if (floor > 0) {
            return "F" + floor;
        } else {
            return "B" + (-floor + 1);
        }
    }

    private void enter(Passenger passenger) {
        TimableOutput.println(String.format("IN-%d-%s-%d", passenger.getId(),
            translate(currentFloor), id));
    }

    private void exit(Passenger passenger) {
        if (currentLoad > 0) { currentLoad--; }
        TimableOutput.println(String.format("OUT-S-%d-%s-%d", passenger.getId(),
            translate(currentFloor), id));
    }

    private void openDoor() throws InterruptedException {
        TimableOutput.println(String.format("OPEN-%s-%d", translate(currentFloor), id));
        Thread.sleep((long) (openTime * 1000));
    }

    private void closeDoor() {
        TimableOutput.println(String.format("CLOSE-%s-%d", translate(currentFloor), id));
    }

    public void setEnd() {
        synchronized (lock) {
            this.isEnd = true;
            lock.notify();
        }
    }

    public Status getStatus() { return status; }

    public void addPassenger(Passenger passenger) {
        passengers.offer(passenger);
    }

    public void addSchedule(Schedule schedule) {
        schedules.offer(schedule);
    }

    private void processSchedule() throws InterruptedException {
        if (schedules.isEmpty()) { return; }
        isScheduled = true;
        TimableOutput.println(String.format("SCHE-BEGIN-%d",id));
        Schedule schedule = schedules.poll();
        targetFloors.clear();
        addTargetFloor(schedule.getToFloor());
        speed = schedule.getSpeed();
        while (currentFloor != schedule.getToFloor()) {
            try {
                move();
                status.updateStatus(currentFloor,getFurtherFloor(),currentLoad);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        openDoor();
        Thread.sleep((long) (600));
        ArrayList<Passenger> tempPassengersIn = new ArrayList<>();
        for (Passenger passenger : passengersIn) {
            if (passenger.getToFloor() == currentFloor) {
                tempPassengersIn.add(passenger);
            }
        }
        for (Passenger passenger : tempPassengersIn) {
            exit(passenger);
        }
        passengersIn.removeAll(tempPassengersIn);
        for (Passenger passenger : passengersIn) {
            exitSchedule(passenger);
            passenger.setFromFloor(translate(currentFloor));
            passengersBack.add(passenger);
        }
        passengersBack.addAll(passengersOut);
        while (!passengers.isEmpty()) {
            passengersBack.add(passengers.poll());
        }
        closeDoor();
        TimableOutput.println(String.format("SCHE-END-%d", id));
        isScheduled = false;
        passengersIn.clear();
        passengersOut.clear();
        targetFloors.clear();
        hasBack = false;
        speed = 0.4;
        currentLoad = 0;
        status.updateStatus(currentFloor,getFurtherFloor(),0);
        synchronized (bigLock) { bigLock.notify(); }
    }

    private void exitSchedule(Passenger passenger) {
        if (currentLoad > 0) { currentLoad--; }
        TimableOutput.println(String.format("OUT-F-%d-%s-%d", passenger.getId(),
            translate(currentFloor), id));
    }

    public Passenger getPassengersBack() {
        if (passengersBack.isEmpty()) { return null; }
        return passengersBack.remove(0);
    }

    public boolean scheduledFinished() {
        if (isScheduled) { return false; }
        if (!schedules.isEmpty()) { return false; }
        return passengersBack.isEmpty();
    }

    private void getUpdate(Update update) {
        if (update.getIdUp() == id) {
            floorDown = update.getTransferFloor();
            status.setFloorDown(floorDown);
            this.type = "A";
        }
        else if (update.getIdDown() == id) {
            floorUp = update.getTransferFloor();
            status.setFloorUp(floorUp);
            this.type = "B";
        }
        targetFloors.clear();
    }

    private void processUpdate() throws InterruptedException {
        if (updates.isEmpty()) { return; }
        isUpdate = true;
        Update update = updates.poll();
        getUpdate(update);
        ArrayList<Passenger> tempPassengersIn = new ArrayList<>();
        ArrayList<Passenger> tempPassengersBack = new ArrayList<>();
        for (Passenger passenger : passengersIn) {
            if (passenger.getToFloor() == currentFloor) {
                tempPassengersIn.add(passenger);
            }
        }
        for (Passenger passenger : passengersIn) {
            if (passenger.getToFloor() != currentFloor) {
                tempPassengersBack.add(passenger);
            }
        }
        if (!passengersIn.isEmpty()) {
            openDoor();
            doorOpened = true;
            for (Passenger passenger : tempPassengersIn) {
                exit(passenger);
            }
            for (Passenger passenger : tempPassengersBack) {
                exitSchedule(passenger);
                passenger.setFromFloor(translate(currentFloor));
            }
            passengersIn.removeAll(tempPassengersIn);
        }
        if (doorOpened) {
            doorOpened = false;
            closeDoor();
        }
        readyUpdate = true;
        synchronized (bigLock) { bigLock.notify(); }
        synchronized (lock) {
            if (readyUpdate) { lock.wait(); }
        }
        Thread.sleep(1000);
        clearUpdate(update,tempPassengersBack);
    }

    public void finishUpdate(Object smallLock) {
        readyUpdate = false;
        this.smallLock = smallLock;
        synchronized (lock) {
            lock.notify();
        }
    }

    public boolean getReadyUpdate() {
        return readyUpdate;
    }

    public void addUpdate(Update update) {
        updates.offer(update);
    }

    public boolean updateFinished() {
        if (!updates.isEmpty()) { return false; }
        if (readyUpdate || isUpdate) { return false; }
        return passengersBack.isEmpty();
    }

    public boolean getIsUpdate() { return isUpdate; }

    public boolean hasBack() {
        if (passengersBack.isEmpty()) { return true; };
        return hasBack;
    }

    public void becomeFriend(Elevator elevator) { this.friend = elevator; }

    private void clearUpdate(Update update,
        ArrayList<Passenger> tempPassengersBack) throws InterruptedException {
        if (update.getIdUp() == id) {
            TimableOutput.println(String.format("UPDATE-END-%d-%d",
                update.getIdUp(), update.getIdDown()));
        }
        else {
            Thread.sleep(50);
        }
        for (Passenger passenger : tempPassengersBack) {
            passengersBack.add(passenger);
        }
        passengersBack.addAll(passengersOut);
        while (!passengers.isEmpty()) {
            passengersBack.add(passengers.poll());
        }
        passengersIn.clear();
        passengersOut.clear();
        targetFloors.clear();
        isUpdate = false;
        speed = 0.2;
        currentLoad = 0;
        if (update.getIdUp() == id) {
            currentFloor = update.getTransferFloor() + 1;
        }
        else {
            currentFloor = update.getTransferFloor() - 1;
        }
        status.updateStatus(currentFloor,getFurtherFloor(),0);
        synchronized (bigLock) {
            bigLock.notify();
        }
    }

    private void processNeedOpen(ArrayList<Passenger> tempPassengersIn,
        ArrayList<Passenger> tempPassengersBack,
        ArrayList<Passenger> tempPassengersOut,Integer temp) {
        for (Passenger passenger : tempPassengersIn) {
            exit(passenger);
        }
        for (Passenger passenger : tempPassengersBack) {
            passenger.setFromFloor(translate(currentFloor));
            exitSchedule(passenger);
            passengersBack.add(passenger);
        }
        for (Passenger passenger : tempPassengersOut) {
            enter(passenger);
            addTargetFloor(passenger.getToFloor());
            passengersIn.add(passenger);
        }
        closeDoor();
        passengersIn.removeAll(tempPassengersIn);
        passengersIn.removeAll(tempPassengersBack);
        passengersOut.removeAll(tempPassengersOut);
        targetFloors.remove(temp);
        if (!passengersBack.isEmpty()) {
            hasBack = false;
        }
    }
}