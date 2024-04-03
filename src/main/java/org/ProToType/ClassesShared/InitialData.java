package org.ProToType.ClassesShared;

public class InitialData {
    public int rv; // login result, response to the client about how the login went
    public int i; // client index so player knows what slot he/she/it is in
    public int mp; // max player amount so client will also know it
    public int tr; // tick rate
    public int up; // udp port
    public PlayerData[] pda; // list of data of players, such as name

    public int getRv() {
        return rv;
    }

    public void setRv(int rv) {
        this.rv = rv;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = mp;
    }

    public int getTr() {
        return tr;
    }

    public void setTr(int tr) {
        this.tr = tr;
    }

    public PlayerData[] getPda() {
        return pda;
    }

    public void setPda(PlayerData[] pda) {
        this.pda = pda;
    }

    public int getUp() {
        return up;
    }

    public void setUp(int up) {
        this.up = up;
    }
}
