import com.oocourse.elevator3.ElevatorInput;
import com.oocourse.elevator3.UpdateRequest;
import com.oocourse.elevator3.Request;
import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.ScheRequest;

public class PassengerFactory implements Runnable {
    private final ElevatorInput elevatorInput;
    private final PassengerQueue passengers;
    private final ScheduleQueue schedules;
    private final UpdateQueue updates;

    public PassengerFactory(ElevatorInput elevatorInput, PassengerQueue passengers
        , ScheduleQueue schedules, UpdateQueue updates) {
        this.elevatorInput = elevatorInput;
        this.passengers = passengers;
        this.schedules = schedules;
        this.updates = updates;
    }

    public boolean createPassenger() {
        Request request = elevatorInput.nextRequest();
        if (request instanceof PersonRequest) {
            Passenger passenger = new Passenger((PersonRequest) request);
            passengers.offer(passenger);
            return true;
        }
        else if (request instanceof ScheRequest) {
            Schedule schedule = new Schedule((ScheRequest) request);
            schedules.offer(schedule);
            return true;
        }
        else if (request instanceof UpdateRequest) {
            Update update = new Update((UpdateRequest) request);
            updates.offer(update);
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        while (true) {
            if (!createPassenger()) {
                passengers.setEnd();
                try {
                    elevatorInput.close();
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }
        try {
            close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 确保你的 close 方法确实关闭了 elevatorInput
    public void close() throws Exception {
        elevatorInput.close();
    }
}
