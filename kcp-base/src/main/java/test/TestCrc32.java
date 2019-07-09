package test;

/**
 * Created by JinMiao
 * 2019-04-16.
 */
public class TestCrc32 {

    //public static void main(String[] args) throws InterruptedException {
    //
    //    CRC32 crc32 = new CRC32();
    //    byte[] bytes = new byte[1024];
    //    crc32.update(bytes);
    //    System.out.println(crc32.getValue());
    //    ByteBuf byteBuf =  PooledByteBufAllocator.DEFAULT.directBuffer(1024);
    //    long now =System.currentTimeMillis();
    //    int i = 0;
    //    byteBuf.writeBytes(new byte[1024]);
    //
    //    ByteBuffer byteBuf1 = byteBuf.nioBuffer();
    //    while (true){
    //        i++;
    //        if(i%10000000==0){
    //            System.out.println(System.currentTimeMillis()-now);
    //            now =System.currentTimeMillis();
    //        }
    //        //crc32.reset();
    //        //crc32.update(bytes);
    //        crc32.reset();
    //        crc32.update(byteBuf1);
    //    }
    //    //System.out.println(crc32.getValue());
    //
    //    //new Thread(() -> {
    //    //    ByteBuf byteBuf =  PooledByteBufAllocator.DEFAULT.directBuffer(20480);
    //    //    //byteBuf.writeInt(5);
    //    //    byteBuf.writeBytes(new byte[20480]);
    //    //    while (true){
    //    //        ByteBuffer byteBuf1 = byteBuf.nioBuffer();
    //    //        //try {
    //    //        //    Thread.sleep(100);
    //    //        //} catch (InterruptedException e) {
    //    //        //    e.printStackTrace();
    //    //        //}
    //    //    }
    //    //}).start();
    //
    //
    //    //Thread.sleep(50000000);
    //}
    ////@Test
    //public void crc32(){
    //    CRC32 crc32 = new CRC32();
    //    byte[] bytes = new byte[1024];
    //    crc32.update(bytes);
    //    System.out.println(crc32.getValue());
    //
    //    //byteBuf1.putInt(0,3);
    //
    //    //((DirectBuffer) byteBuf1).cleaner().clean();
    //    //System.out.println(byteBuf1.getInt(0));
    //    //System.out.println(byteBuf.getInt(0));
    //
    //    //System.out.println();
    //
    //
    //
    //
    //    //for (int i = 0; i < 20000000; i++) {
    //    //    crc32.update("abcdfg".getBytes());
    //    //    crc32.getValue();
    //    //}
    //    //long start = System.nanoTime();
    //    //for (int i = 0; i < 20000000; i++) {
    //    //    crc32.update("abcdfg".getBytes());
    //    //    crc32.getValue();
    //    //}
    //
    //    //System.out.println((System.nanoTime()-start)/20000000.0);
    //}
}
