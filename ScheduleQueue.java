import java.util.ArrayList;

public class ScheduleQueue {
    private final ArrayList<Schedule> schedules;
    private final Object lock;

    public ScheduleQueue(Object lock) {
        schedules = new ArrayList<>();
        this.lock = lock;
    }

    public synchronized void offer(Schedule schedule) {
        synchronized (lock) {
            schedules.add(schedule);
            lock.notify();
        }
    }

    public synchronized Schedule poll() {
        if (schedules.isEmpty()) {
            return null;
        }
        return schedules.remove(0);
    }

    public synchronized boolean isEmpty() {
        return schedules.isEmpty();
    }
}
