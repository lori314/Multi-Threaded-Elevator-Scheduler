import com.oocourse.elevator3.TimableOutput;

import java.util.ArrayList;
import java.util.HashMap;

public class Assigner implements Runnable {
    private final PassengerQueue passengers;
    private final ArrayList<Passenger> passengerList;
    private final ArrayList<Update> updateList;
    private final HashMap<Integer, Elevator> elevators;
    private final ArrayList<Integer> isScheduled;
    private final ArrayList<Integer> isUpdated;
    private final ArrayList<Integer> isSending;
    private final ArrayList<Integer> finishUpdate;
    private final ScheduleQueue schedules;
    private final UpdateQueue updates;
    private final Object lock;

    public Assigner(PassengerQueue passengers, HashMap<Integer, Elevator> elevators,
        ScheduleQueue schedules, UpdateQueue updates) {
        this.passengers = passengers;
        this.passengerList = new ArrayList<>();
        this.updateList = new ArrayList<>();
        this.isScheduled = new ArrayList<>();
        this.isUpdated = new ArrayList<>();
        this.elevators = elevators;
        this.schedules = schedules;
        this.updates = updates;
        this.lock = passengers.getLock();
        this.isSending = new ArrayList<>();
        this.finishUpdate = new ArrayList<>();
    }

    @Override
    public void run() {
        while (true) {
            boolean bool = passengers.isEmpty() && passengers.isEnd() &&
                isSending.isEmpty() && finishUpdate.isEmpty();
            if (bool && isScheduled.isEmpty() && schedules.isEmpty() &&
                passengerList.isEmpty() && isUpdated.isEmpty()) {
                for (Elevator elevator : elevators.values()) {
                    elevator.setEnd();
                }
                break;
            }
            if (passengers.isEmpty() && updates.isEmpty() &&
                schedules.isEmpty()) {
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            processSchedule();

            if (!updates.isEmpty()) {
                Update update = updates.poll();
                elevators.get(update.getIdDown()).addUpdate(update);
                elevators.get(update.getIdUp()).addUpdate(update);
                isUpdated.add(update.getIdDown());
                isUpdated.add(update.getIdUp());
                finishUpdate.add(update.getIdDown());
                finishUpdate.add(update.getIdUp());
                updateList.add(update);
            }

            processUpdate();
            processIsSending();

            ArrayList<Passenger> tempPassengers = new ArrayList<>();
            for (Passenger passenger : passengerList) {
                if (assign(passenger)) {
                    tempPassengers.add(passenger);
                }
            }
            passengerList.removeAll(tempPassengers);
            Passenger passenger = passengers.poll();
            if (passenger == null) {
                continue;
            }
            if (!assign(passenger)) {
                passengerList.add(passenger);
            }
        }
    }

    public boolean assign(Passenger passenger) {
        int distance = 114514;
        int id = 114514;
        for (int i = 1;i <= 6;i++) {
            if (!elevators.get(i).scheduledFinished()) {
                continue;
            }
            if (!elevators.get(i).updateFinished()) {
                continue;
            }
            Status status = elevators.get(i).getStatus();
            if (canPick(status, passenger)) {
                if (distance > Math.abs(status.getCurrentFloor() - passenger.getFromFloor())) {
                    distance = Math.abs(status.getCurrentFloor() - passenger.getFromFloor());
                    id = i;
                }
                if (status.isUpdated()) {
                    id = i;
                    break;
                }
            }
        }
        if (id == 114514) {
            for (int i = 1;i <= 6;i++) {
                if (!elevators.get(i).scheduledFinished()) {
                    continue;
                }
                if (!elevators.get(i).updateFinished()) {
                    continue;
                }
                Status status = elevators.get(i).getStatus();
                if (canSend(status, passenger)) {
                    id = i;
                    isSending.add(i);
                    break;
                }
            }
        }
        if (id != 114514) {
            TimableOutput.println(String.format("RECEIVE-%d-%d", passenger.getId(), id));
            elevators.get(id).addPassenger(passenger);
            elevators.get(id).getStatus().addPassenger();
            return true;
        }
        return false;
    }

    private boolean canPick(Status status, Passenger passenger) {
        if (!status.inRange(passenger.getFromFloor(), passenger.getToFloor())) {
            return false;
        }
        else if (status.getCurrentLoad() >= 6) {
            return false;
        }
        else if (status.isWait()) {
            return true;
        }
        else if (status.getDirection().equals(passenger.getDirection())) {
            if (status.getDirection().equals("UP") &&
                passenger.getFromFloor() >= status.getCurrentFloor()) {
                return true;
            }
            else if (status.getDirection().equals("DOWN") &&
                passenger.getFromFloor() <= status.getCurrentFloor()) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean canSend(Status status, Passenger passenger) {
        if (!status.sendAble(passenger)) {
            return false;
        }
        else if (status.getCurrentLoad() >= 6) {
            return false;
        }
        else if (status.isWait()) {
            return true;
        }
        else if (status.getDirection().equals(passenger.getDirection())) {
            if (status.getDirection().equals("UP") &&
                passenger.getFromFloor() >= status.getCurrentFloor()) {
                return true;
            }
            else if (status.getDirection().equals("DOWN") &&
                passenger.getFromFloor() <= status.getCurrentFloor()) {
                return true;
            }
            return false;
        }
        return false;
    }

    private void processSchedule() {
        if (!schedules.isEmpty()) {
            Schedule schedule = schedules.poll();
            elevators.get(schedule.getId()).addSchedule(schedule);
            isScheduled.add(schedule.getId());
        }
        if (!isScheduled.isEmpty()) {
            ArrayList<Integer> temp = new ArrayList<>();
            for (int id : isScheduled) {
                while (true) {
                    Passenger passenger = elevators.get(id).getPassengersBack();
                    if (passenger == null) {
                        break;
                    }
                    passengerList.add(passenger);
                }
                if (elevators.get(id).scheduledFinished()) {
                    temp.add(id);
                    while (isSending.contains(id)) {
                        isSending.remove((Integer) id);
                    }
                }
            }
            isScheduled.removeAll(temp);
        }
    }

    private void processUpdate() {
        if (!isUpdated.isEmpty()) {
            ArrayList<Integer> temp = new ArrayList<>();
            for (int id : isUpdated) {
                if (elevators.get(id).getReadyUpdate()) {
                    temp.add(id);
                }
            }
            while (temp.size() >= 2) {
                ArrayList<Update> temp1 = new ArrayList<>();
                int num = temp.get(0);
                for (Update update : updateList) {
                    if (update.getIdDown() == num || update.getIdUp() == num) {
                        if (update.getIdDown() == num) {
                            if (!temp.contains(update.getIdUp())) {
                                continue;
                            }
                        }
                        else if (update.getIdUp() == num) {
                            if (!temp.contains(update.getIdDown())) {
                                continue;
                            }
                        }
                        temp1.add(update);
                        Object lock = new Object();
                        TimableOutput.println(String.format("UPDATE-BEGIN-%d-%d",
                            update.getIdUp(), update.getIdDown()));
                        elevators.get(update.getIdDown()).finishUpdate(lock);
                        elevators.get(update.getIdDown()).becomeFriend(
                            elevators.get(update.getIdUp()));
                        elevators.get(update.getIdUp()).finishUpdate(lock);
                        elevators.get(update.getIdUp()).becomeFriend(
                            elevators.get(update.getIdDown()));
                        temp.remove((Integer) update.getIdDown());
                        temp.remove((Integer) update.getIdUp());
                        isUpdated.remove((Integer) update.getIdDown());
                        isUpdated.remove((Integer) update.getIdUp());
                        break;
                    }
                }
                if (temp1.isEmpty()) {
                    break;
                }
                updateList.removeAll(temp1);
            }
        }
        for (int id = 1;id <= 6;id++) {
            if (!isUpdated.contains(id) && !elevators.get(id).getIsUpdate()
                && !isScheduled.contains(id) && !isSending.contains(id)
                && finishUpdate.contains(id)) {
                while (true) {
                    Passenger passenger = elevators.get(id).getPassengersBack();
                    if (passenger == null) { break; }
                    passengerList.add(passenger);
                }
                finishUpdate.remove((Integer)id);
            }
        }
    }

    private void processIsSending() {
        if (!isSending.isEmpty()) {
            ArrayList<Integer> temp = new ArrayList<>();
            for (int id : isSending) {
                if (elevators.get(id).hasBack()) {
                    continue;
                }
                while (true) {
                    Passenger passenger = elevators.get(id).getPassengersBack();
                    if (passenger == null) {
                        break;
                    }
                    passengerList.add(passenger);
                    temp.add(id);
                }
            }
            for (int id : temp) {
                isSending.remove((Integer) id);
            }
        }
    }
}
