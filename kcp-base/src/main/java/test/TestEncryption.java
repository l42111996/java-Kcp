package test;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * Created by JinMiao
 * 2019-06-24.
 */
public class TestEncryption {

    private Cipher encruyptCipher;

    private Cipher decryptCipher;
    public void init(){
        String password = "SHA1PRNG";
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");

            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password.getBytes());
            kgen.init(128, random);// 利用用户密码作为随机数初始化出
            SecretKey secretKey = kgen.generateKey();// 根据用户密码，生成一个密钥
            byte[] enCodeFormat = secretKey.getEncoded();// 返回基本编码格式的密钥，如果此密钥不支持编码，则返回
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");// 转换为AES专用密钥


            encruyptCipher = Cipher.getInstance("AES");// 创建密码器
            encruyptCipher.init(Cipher.ENCRYPT_MODE, key);// 初始化为加密模式的密码器

            decryptCipher = Cipher.getInstance("AES");// 创建密码器
            decryptCipher.init(Cipher.DECRYPT_MODE, key);// 初始化为解密模式的密码器
        }catch (Exception e){
            e.printStackTrace();
        }

    }




    public void encrypt(ByteBuffer in,ByteBuffer out) throws BadPaddingException, ShortBufferException, IllegalBlockSizeException {
        encruyptCipher.doFinal(in,out);
    }


    public void decrypt(ByteBuffer in,ByteBuffer out) throws BadPaddingException, ShortBufferException, IllegalBlockSizeException {
        decryptCipher.doFinal(in,out);
    }


    //public static void main(String[] args) throws BadPaddingException, ShortBufferException, IllegalBlockSizeException {
    //    test.TestEncryption testEncryption = new test.TestEncryption();
    //    testEncryption.init();
    //    ByteBuffer in = ByteBuffer.allocate(1024);
    //    ByteBuffer out = ByteBuffer.allocate(1024);
    //
    //    for (int i = 0; i < 100; i++) {
    //        in.putInt(i);
    //    }
    //
    //    testEncryption.encruyptCipher.doFinal(in,out);
    //
    //
    //
    //    byte[] outBytes = testEncryption.encruyptCipher.doFinal(in.array());
    //    byte[] inBytes = testEncryption.decryptCipher.doFinal(outBytes);
    //
    //
    //
    //    while (in.hasRemaining()){
    //        System.out.println(in.getInt());
    //    }
    //
    //
    //
    //
    //
    //    System.out.println();
    //
    //
    //
    //
    //
    //}
}
