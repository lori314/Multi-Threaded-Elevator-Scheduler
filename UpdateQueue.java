import java.util.ArrayList;

public class UpdateQueue {
    private final ArrayList<Update> updates;
    private final Object lock;

    public UpdateQueue(Object lock) {
        updates = new ArrayList<>();
        this.lock = lock;
    }

    public synchronized void offer(Update update) {
        synchronized (lock) {
            updates.add(update);
            lock.notify();
        }
    }

    public synchronized Update poll() {
        if (updates.isEmpty()) {
            return null;
        }
        return updates.remove(0);
    }

    public synchronized boolean isEmpty() {
        return updates.isEmpty();
    }
}
