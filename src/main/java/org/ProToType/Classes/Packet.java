package org.ProToType.Classes;

public class Packet {
    public int type;
    public Player owner;
    public String json;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
