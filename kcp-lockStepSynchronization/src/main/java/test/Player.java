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
    private IWriter iWriter;

    private int id ;

    private List<ByteBuf> messages= new ArrayList<>();


    public Player(IWriter iWriter) {
        this.iWriter = iWriter;
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


    public void write(ByteBuf byteBuf){
        iWriter.write(byteBuf);
    }





}
