import com.oocourse.elevator3.ElevatorInput;
import com.oocourse.elevator3.TimableOutput;

import java.util.HashMap;

public class Manager {
    public void start() {
        TimableOutput.initStartTimestamp();
        ElevatorInput elevatorInput = new ElevatorInput(System.in);
        Object bigLock = new Object();
        PassengerQueue queue = new PassengerQueue(bigLock);
        ScheduleQueue schedule = new ScheduleQueue(bigLock);
        UpdateQueue update = new UpdateQueue(bigLock);
        PassengerFactory factory = new PassengerFactory(elevatorInput,queue,schedule,update);
        Thread input = new Thread(factory);
        input.start();
        HashMap<Integer,Elevator> elevators = new HashMap<>();
        HashMap<Integer,Thread> elevatorThreads = new HashMap<>();
        for (int i = 1; i <= 6; i++) {
            Object lock = new Object();
            elevators.put(i,new Elevator(new PassengerQueue(lock),
                new ScheduleQueue(lock),new UpdateQueue(lock),i,bigLock));
            elevatorThreads.put(i,new Thread(elevators.get(i)));
            elevatorThreads.get(i).start();
        }
        Assigner assigner = new Assigner(queue,elevators,schedule,update);
        Thread assign = new Thread(assigner);
        assign.start();
    }
}
