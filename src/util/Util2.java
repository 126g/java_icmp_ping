package util;

public class Util2 {
	/**
	 * 将6字节的数组转换成MAC地址形式的字符串
	 * 
	 * @param bs
	 *            长度为6的字节数组
	 * @return
	 */
	public static String bytes2MacAddress(byte[] bs) {
		if (bs == null || bs.length != 6) {
			return "00:00:00:00:00:00";
		}

		StringBuilder macAddress = new StringBuilder();

		for (int i = 0; i < bs.length; i++) {
			String t = Integer.toHexString(bs[i] & 0xff);
			if (t.length() == 1) {
				macAddress.append("0");
			}
			macAddress.append(t);

			if (i != bs.length - 1) {
				macAddress.append(":");
			}
		}
		return macAddress.toString();
	}

	/**
	 * 将字符串形式的mac地址转换成字节数组
	 * 
	 * @param macAddress
	 * @return
	 */
	public static byte[] macAddress2bytes(String macAddress) {
		String mac = macAddress.replace("-", "");
		mac = mac.replace(":", "");
		if (mac.length() != 12) {
			return null;
		}
		return hex2Bytes(mac);
	}

	/**
	 * 将字符串形式的十六进制数组转换成字节数组
	 * 
	 * @param src
	 * @return
	 */
	public static byte[] hex2Bytes(String src) {
		byte[] res = new byte[src.length() / 2];
		char[] chs = src.toCharArray();
		int[] b = new int[2];

		for (int i = 0, c = 0; i < chs.length; i += 2, c++) {
			for (int j = 0; j < 2; j++) {
				if (chs[i + j] >= '0' && chs[i + j] <= '9') {
					b[j] = (chs[i + j] - '0');
				} else if (chs[i + j] >= 'A' && chs[i + j] <= 'F') {
					b[j] = (chs[i + j] - 'A' + 10);
				} else if (chs[i + j] >= 'a' && chs[i + j] <= 'f') {
					b[j] = (chs[i + j] - 'a' + 10);
				}
			}

			b[0] = (b[0] & 0x0f) << 4;
			b[1] = (b[1] & 0x0f);
			res[c] = (byte) (b[0] | b[1]);
		}

		return res;
	}

}
