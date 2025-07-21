import java.util.ArrayList;

public class Strategy {
    public boolean isConflict(int nextFloor,Elevator friend,String type,int floorDown,int floorUp) {
        if (friend == null) { return false; }

        Status friendStatus = friend.getStatus();
        int friendFloor = friendStatus.getCurrentFloor();
        int friendTarget = friend.getFurtherFloor();
        int friendDirection = Integer.compare(friendTarget, friendFloor);
        int friendNextFloor = friendFloor; // Calculate friend's next potential move
        if (friendDirection > 0) { friendNextFloor = friendFloor + 1; }
        else if (friendDirection < 0) { friendNextFloor = friendFloor - 1; }

        // --- 规则 1: 目标楼层已被对方占用 ---
        if (nextFloor == friendFloor) {
            return true;
        }

        // --- 规则 2: 移动后会违反 A >= B ---
        if (type.equals("A") && nextFloor < friendFloor) {
            return true;
        }
        if (type.equals("B") && nextFloor > friendFloor) {
            return true;
        }

        // --- 规则 3: 换乘楼层协调 (A 优先) ---
        int transferFloor = type.equals("A") ? floorDown : floorUp; // The boundary floor

        // Case 3.1: 我要去换乘楼层
        if (nextFloor == transferFloor) {
            if (friendFloor == transferFloor) {
                return true;
            }
            // 如果对方也正要移动到换乘层
            if (friendNextFloor == transferFloor) {
                if (type.equals("B")) {
                    return true; // B 等待 A
                } else {
                    return false;
                    // A 不等待，继续执行
                }
            }
        }
        return false; // 默认无冲突
    }

    public int getFurtherFloor(ArrayList<Integer> targetFloors,int currentFloor) {
        if (targetFloors.isEmpty()) {
            return currentFloor; //this sentence will never execute;
        }
        else {
            if (targetFloors.get(0) == currentFloor) {
                if (targetFloors.size() > 1) {
                    int temp1 = targetFloors.get(1);
                    int temp0 = targetFloors.get(0);
                    targetFloors.set(0, temp0);
                    targetFloors.set(1, temp1);
                }
            }
            if (targetFloors.get(0) > currentFloor) {
                int highestFloor = targetFloors.get(0);
                for (int i = 1; i < targetFloors.size(); i++) {
                    if (targetFloors.get(i) > highestFloor) {
                        highestFloor = targetFloors.get(i);
                    }
                }
                return highestFloor;
            }
            else {
                int lowestFloor = targetFloors.get(0);
                for (int i = 1; i < targetFloors.size(); i++) {
                    if (targetFloors.get(i) < lowestFloor) {
                        lowestFloor = targetFloors.get(i);
                    }
                }
                return lowestFloor;
            }
        }
    }
}
