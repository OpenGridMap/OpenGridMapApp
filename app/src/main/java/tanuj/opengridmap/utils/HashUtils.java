package tanuj.opengridmap.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Tanuj on 20/10/2015.
 */
public class HashUtils {
    public static byte[] sha256(String str) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(str.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return md.digest();
    }

    public static String getSha256String(String str) {
        return String.format("%064x", new java.math.BigInteger(1, sha256(str)));
    }

    public static long getSha256Long(String str) {
        return new BigInteger(sha256(str)).longValue();
    }
}
