import com.oocourse.elevator3.UpdateRequest;

public class Update {
    private final int idUp;
    private final int idDown;
    private final String transferFloor;

    public Update(UpdateRequest updateRequest) {
        this.idUp = updateRequest.getElevatorAId();
        this.idDown = updateRequest.getElevatorBId();
        this.transferFloor = updateRequest.getTransferFloor();
    }

    public int getIdUp() {
        return idUp;
    }

    public int getIdDown() {
        return idDown;
    }

    public int getTransferFloor() {
        int sign = 0;
        if (transferFloor.charAt(0) == 'F') {
            sign = 1;
        }
        else if (transferFloor.charAt(0) == 'B') {
            sign = -1;
        }
        int num = Integer.parseInt(transferFloor.substring(1));
        if (sign == -1) {
            num = num - 1;
        }
        return num * sign;
    }
}
