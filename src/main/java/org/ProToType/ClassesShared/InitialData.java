package org.ProToType.ClassesShared;

public class InitialData {
    public int loginResultValue; // login result, response to the client about how the login went
    public int index; // client index so player knows what slot he/she/it is in
    public int maxPlayers; // max player amount so client will also know it
    public int tickRate; // tick rate
    public PlayerData[] playersData;

    public int getLoginResultValue() {
        return loginResultValue;
    }

    public void setLoginResultValue(int loginResultValue) {
        this.loginResultValue = loginResultValue;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getTickRate() {
        return tickRate;
    }

    public void setTickRate(int tickRate) {
        this.tickRate = tickRate;
    }

    public PlayerData[] getPlayersData() {
        return playersData;
    }

    public void setPlayersData(PlayerData[] playersData) {
        this.playersData = playersData;
    }
}
