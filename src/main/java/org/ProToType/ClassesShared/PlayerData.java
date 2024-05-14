package org.ProToType.ClassesShared;

public class PlayerData {
    public int i = -1; // player index
    public int s; // status of player connection
    public String un = "Unnamed"; // username

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public int getS() {
        return s;
    }

    public void setS(int s) {
        this.s = s;
    }

    public String getUn() {
        return un;
    }

    public void setUn(String un) {
        this.un = un;
    }
}
