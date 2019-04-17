import org.junit.Test;

import java.util.zip.CRC32;

/**
 * Created by JinMiao
 * 2019-04-16.
 */
public class TestCrc32 {
    @Test
    public void crc32(){
        CRC32 crc32 = new CRC32();
        byte[] bytes = new byte[1024];
        crc32.update(bytes);
        System.out.println(crc32.getValue());
        //for (int i = 0; i < 20000000; i++) {
        //    crc32.update("abcdfg".getBytes());
        //    crc32.getValue();
        //}
        //long start = System.nanoTime();
        //for (int i = 0; i < 20000000; i++) {
        //    crc32.update("abcdfg".getBytes());
        //    crc32.getValue();
        //}

        //System.out.println((System.nanoTime()-start)/20000000.0);
    }
}
