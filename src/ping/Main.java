package ping;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.packet.EthernetPacket;
import jpcap.packet.ICMPPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;
import util.Util;

public class Main {

	public static void main(String[] args) {

		NetworkInterface[] devices = JpcapCaptor.getDeviceList();

		InetAddress ip = null;
		String strip = "";

		if (args.length > 0) {
			strip = args[0];
		} else {
			System.out.print("请输入域名或者IP地址：");
			Scanner sc = new Scanner(System.in);
			strip = sc.nextLine();
			sc.close();
		}

		try {
			ip = InetAddress.getByName(strip);
		} catch (UnknownHostException e) {
			System.out.println("无法找到主机：" + strip);
			System.exit(0);
		}

		if (args.length >= 3 && args[1].toLowerCase().equals("-n")) {
			try {
				int times = Integer.parseInt(args[2]);
				ping(devices[0], ip, times);
			} catch (NumberFormatException e) {
				ping(devices[0], ip, 4);
			}

		} else {
			ping(devices[0], ip, 4);
		}

	}

	public static void ping(NetworkInterface device, InetAddress dstIP, int times) {
		InetAddress srcIP = null;

		for (NetworkInterfaceAddress addr : device.addresses) {
			if (addr.address instanceof Inet4Address) {
				srcIP = addr.address;
				break;
			}
		}


		int timeout = 1000;
		JpcapCaptor captor = null;
		try {
			captor = JpcapCaptor.openDevice(device, 2000, false, timeout);
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte[] defaultGatewayMacAddress = null;
		InetAddress baiduIP = null;
		try {
			baiduIP = InetAddress.getByName("www.baidu.com");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		while (true) {
			try {
				new URL("http://www.baidu.com").openStream().close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Packet p = captor.getPacket();

			if (p == null) {
				continue;
			}

			IPPacket packet = (IPPacket) p;

			if (packet == null) {
				System.out.println("[调试信息]获取默认网关MAC地址失败，正在重试。。。");
			} else {
				if (packet.dst_ip.getHostAddress().equals(baiduIP.getHostAddress())) {
					defaultGatewayMacAddress = ((EthernetPacket) packet.datalink).dst_mac;
					break;
				}
			}
		}

		System.out.println("[调试信息]默认网关的MAC地址：" + Util.bytes2MacAddress(defaultGatewayMacAddress));
		System.out.println();
		System.out.println();

		try {
			captor.setFilter("icmp and src host " + dstIP.getHostAddress(), true);
		} catch (IOException e) {
		}

		byte[] data = "abcdefghijklmnopqrstuvwxyz123456".getBytes();
		JpcapSender sender = captor.getJpcapSenderInstance();
		short seq = 1;

		System.out.println(String.format("正在 Ping %s 具有 %d 字节的数据：", dstIP.getHostAddress(), data.length));

		int send = 0;
		int recv = 0;
		int timeSum = 0;
		int maxTime = 0;
		int minTime = 99999;
		String strip = "";

		for (int i = 0; i < times; i++) {

			ICMPPacket icmp = new ICMPPacket();
			icmp.type = ICMPPacket.ICMP_ECHO;
			icmp.code = 0;
			icmp.seq = seq++;
			icmp.id = 1;
			icmp.data = data;
			int ttl = 128;
			icmp.setIPv4Parameter(0, false, false, false, 0, false, false, false, 0, 0, ttl, IPPacket.IPPROTO_ICMP,
					srcIP, dstIP);
			EthernetPacket eth = new EthernetPacket();
			eth.frametype = EthernetPacket.ETHERTYPE_IP;
			eth.src_mac = device.mac_address;
			eth.dst_mac = defaultGatewayMacAddress;
			icmp.datalink = eth;
			long time = System.currentTimeMillis();
			icmp.sec = time / 1000;
			icmp.usec = time % 1000 * 1000;

			sender.sendPacket(icmp);
			send++;

			ICMPPacket packet = (ICMPPacket) captor.getPacket();

			if (packet == null) {
				System.out.println("请求超时。");
			} else {
				recv++;
				long t1 = icmp.sec * 1000 + icmp.usec / 1000;
				long t2 = packet.sec * 1000 + packet.usec / 1000;
				InetAddress ip = packet.src_ip;
				int dataLength = packet.data.length;
				int hop_limit = packet.hop_limit;
				int t = (int) (t2 - t1);
				if (t < 0) {
					t = 10;
				}

				String status = String.format("来自 %s 的回复：字节=%d 时间=%dms TTL=%d", ip.getHostAddress(), dataLength, t,
						hop_limit);
				System.out.println(status);

				timeSum += t;
				strip = ip.getHostAddress();
				if (t > maxTime) {
					maxTime = t;
				}
				if (t < minTime) {
					minTime = t;
				}
			}
		}

		String result1 = String.format("%s 的 Ping 统计信息：", strip);
		System.out.println(result1);
		int lost = send - recv;
		int l = (int) (((double) lost / send) * 100);
		String result2 = String.format("　　数据包：已发送=%d，已接收=%d，丢失=%d（%d%% 丢失）", send, recv, lost, l);
		System.out.println(result2);

		if (recv > 0) {
			System.out.println("往返行程的估计时间(以毫秒为单位):");
			int avg = timeSum / recv;
			String result3 = String.format("　　最短 = %dms，最长 = %dms，平均 = %dms", minTime, maxTime, avg);
			System.out.println(result3);
		}
		System.out.println();
		int a = ICMPPacket.ICMP_TIMXCEED;
		int b = ICMPPacket.ICMP_UNREACH;
		int c = ICMPPacket.ICMP_ECHOREPLY;
	}
}
