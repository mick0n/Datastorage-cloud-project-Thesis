package com.mnorrman.datastorageproject.tools;

public class HexConverter {

	private static final char[] hexBytes = new char[] { '0', '1', '2', '3',
			'4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static String toHex(byte[] array) {
		StringBuffer buff = new StringBuffer();
		for (int a = 0; a < array.length; a++) {
			int temp = array[a] & 0xff;
			buff.append(hexBytes[temp >> 4]);
			buff.append(hexBytes[temp & 0xF]);
		}
		return buff.toString();
	}

	public static byte[] toByte(String hex) {
		byte[] buff = new byte[hex.length() / 2];
		for (int i = 0; i < hex.length(); i += 2) {
			buff[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character
					.digit(hex.charAt(i + 1), 16));
		}
		return buff;
	}
}
