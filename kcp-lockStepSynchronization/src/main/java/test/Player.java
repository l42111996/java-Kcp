package test;

import io.netty.buffer.ByteBuf;
import kcp.Ukcp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by JinMiao
 * 2019-07-08.
 */
public class Player {

    private static final AtomicInteger idGen = new AtomicInteger();
    private Ukcp ukcp;

    private int id ;

    private List<ByteBuf> messages= new ArrayList<>();


    public Player(Ukcp ukcp) {
        this.ukcp = ukcp;
        this.id = idGen.incrementAndGet();
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<ByteBuf> getMessages() {
        return messages;
    }

    public void setMessages(List<ByteBuf> messages) {
        this.messages = messages;
    }

    public Ukcp getUkcp() {
        return ukcp;
    }

    public void setUkcp(Ukcp ukcp) {
        this.ukcp = ukcp;
    }
}
