package yau.src.metronome.models;

public class ServerMessage {
    public String sender;
    public Long t2 = System.currentTimeMillis();
    public Long t1 = 0L;
    public String message = null;
    public ClientMessage clientMessage = null;

    public void setT1(Long t1) {
        this.t1 = t1;
    }

    public void setT2(Long t2) {
        this.t2 = t2;
    }

    public ServerMessage(String sender) {
        this.sender = sender;
    }

    public ServerMessage(String sender, ClientMessage clientMessage) {
        this.sender = sender;
        this.clientMessage = clientMessage;
    }

    public ServerMessage(String sender, String message, ClientMessage clientMessage) {
        this.sender = sender;
        this.message = message;
        this.clientMessage = clientMessage;
    }
}
