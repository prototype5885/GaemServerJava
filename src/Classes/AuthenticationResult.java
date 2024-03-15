package Classes;

import java.net.Socket;

public class AuthenticationResult {
    public byte result;
    public int dbindex;
    public int slotIndex;
    public Socket tcpClientSocket;
    public String playerName;

    public byte getResult() {
        return result;
    }

    public void setResult(byte result) {
        this.result = result;
    }

    public int getDbindex() {
        return dbindex;
    }

    public void setDbindex(int dbindex) {
        this.dbindex = dbindex;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public Socket getTcpClientSocket() {
        return tcpClientSocket;
    }

    public void setTcpClientSocket(Socket tcpClientSocket) {
        this.tcpClientSocket = tcpClientSocket;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
