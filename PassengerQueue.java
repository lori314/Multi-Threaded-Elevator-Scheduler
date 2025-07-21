import java.util.ArrayList;

public class PassengerQueue {
    private final ArrayList<Passenger> passengers;
    private boolean isEnd;
    private final Object lock;

    public PassengerQueue(Object lock) {
        this.passengers = new ArrayList<>();
        this.lock = lock;
    }

    public synchronized void offer(Passenger passenger) {
        synchronized (lock) {
            passengers.add(passenger);
            lock.notify();
        }
    }

    public synchronized Passenger poll() {
        if (passengers.isEmpty()) {
            return null;
        }
        return passengers.remove(findMaxPriority());
    }

    private synchronized int findMaxPriority() {
        int maxPriority = -1;
        int maxPriorityIndex = 0;
        for (int i = 0; i < passengers.size(); i++) {
            Passenger passenger = passengers.get(i);
            if (passenger.getPriority() > maxPriority) {
                maxPriority = passenger.getPriority();
                maxPriorityIndex = i;
            }
        }
        return maxPriorityIndex;
    }

    public synchronized void setEnd() {
        synchronized (lock) {
            isEnd = true;
            lock.notify();
        }
    }

    public synchronized boolean isEnd() {
        return isEnd;
    }

    public synchronized boolean isEmpty() {
        return passengers.isEmpty();
    }

    public Object getLock() {
        return lock;
    }
}
