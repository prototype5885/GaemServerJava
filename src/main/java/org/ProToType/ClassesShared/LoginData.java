package org.ProToType.ClassesShared;

public class LoginData {
    public boolean reg; // true means player wants to register
    public String un; // player name
    public String pw; // password

    public boolean isReg() {
        return reg;
    }

    public void setReg(boolean reg) {
        this.reg = reg;
    }

    public String getUn() {
        return un;
    }

    public void setUn(String un) {
        this.un = un;
    }

    public String getPw() {
        return pw;
    }

    public void setPw(String pw) {
        this.pw = pw;
    }
}
