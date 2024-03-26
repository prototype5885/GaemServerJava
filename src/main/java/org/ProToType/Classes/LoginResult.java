package org.ProToType.Classes;

import java.net.Socket;

public class LoginResult {
    public int resultValue;
    public int dbindex;
    public String playerName;


    public int getDbindex() {
        return dbindex;
    }

    public void setDbindex(int dbindex) {
        this.dbindex = dbindex;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getResultValue() {
        return resultValue;
    }

    public void setResultValue(int resultValue) {
        this.resultValue = resultValue;
    }
}
