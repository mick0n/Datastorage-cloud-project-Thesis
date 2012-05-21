package com.mnorrman.datastorageproject.tools;

/**
 * Utility class for converting between bytes and hex
 *
 * @author Mikael Norrman
 */
public class HexConverter {

    private static final char[] hexBytes = new char[]{'0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String toHex(byte[] array) {
        StringBuffer buff = new StringBuffer();
        for (int a = 0; a < array.length; a++) {
            int temp = array[a] & 0xff;
            buff.append(hexBytes[temp >> 4]);
            buff.append(hexBytes[temp & 0xF]);
        }
        return buff.toString();
    }

    public static String toHex(long longValue){
        return String.format("%8s", Long.toHexString(longValue).toUpperCase()).replace(' ', '0');
    }
    
    public static String toHex(int integer) {
        return String.format("%8s", Integer.toHexString(integer).toUpperCase()).replace(' ', '0');
    }

    public static String toHex(short shortValue) {
        return String.format("%4s", Integer.toHexString(shortValue).toUpperCase()).replace(' ', '0');
    }

    public static byte[] toByte(String hex) {
        byte[] buff = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            buff[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return buff;
    }

    public static int toInt(String hex) {
        return (int) Long.parseLong(hex, 16);
    }
    
    public static short toShort(String hex) {
        return (short) Integer.parseInt(hex, 16);
    }
    
    public static long toLong(String hex){
        return Long.parseLong(hex, 16);
    }
}
