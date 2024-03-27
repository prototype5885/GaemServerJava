package org.ProToType.ClassesShared;

import org.ProToType.Classes.ConnectedPlayer;

public class PlayerPosition {
    public int i;
    public float x;
    public float y;
    public float z;
    public float rx;
    public float ry;

    public void UpdatePlayerPosition(PlayerPosition playerPosition) {
        this.x = playerPosition.x;
        this.y = playerPosition.y;
        this.z = playerPosition.z;
        this.rx = playerPosition.rx;
        this.ry = playerPosition.ry;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float getRx() {
        return rx;
    }

    public void setRx(float rx) {
        this.rx = rx;
    }

    public float getRy() {
        return ry;
    }

    public void setRy(float ry) {
        this.ry = ry;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }
}
