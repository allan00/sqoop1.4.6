package org.ping.apache.sqoop.mapreduce.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

/**
 * 该类为多线程类，用于服务端
 */
public class ServerThread implements Runnable {

	private Socket client = null;

	public ServerThread(Socket client) {
		this.client = client;
	}

	@Override
	public void run() {
		try {
			// 获取Socket的输出流，用来向客户端发送数据
			PrintStream out = new PrintStream(client.getOutputStream());
			// 获取Socket的输入流，用来接收从客户端发送过来的数据
			BufferedReader buf = new BufferedReader(new InputStreamReader(
					client.getInputStream()));
			InputStream in = client.getInputStream();
			boolean flag = true;
			Process p = null;
			int len = 0;
			while (flag) {
				// 接收从客户端发送过来的数据
				String str = buf.readLine();
				if (str == null || "".equals(str)||"bye".equals(str)) {
					flag = false;
				} else if ("prepare to send signature".equals(str)) {
					sendInfo(client, "prepared to receive signature");
					OutputStream sigOutStream = new FileOutputStream(new File(
							"/home/ping/test/mut.0.sig"));
					byte[] bufFile = new byte[1024 * 1024]; // 接收数据的缓存

					while (len != -1) {
						len = in.read(bufFile); // 接收数据
						sigOutStream.write(bufFile, 0, len); // 写入硬盘文件
					}
					sendInfo(client, "receive sig finished");
				} else if (str.contains("mysqldump")) {
					// 将接收到到命令，进行处理
					String command = parse(str);
					p = Runtime.getRuntime().exec(command);
					InputStream is = p.getInputStream();
					FileOutputStream os = new FileOutputStream(new File(
							"/tmp/tmp.txt"));
					BufferedReader r = null;
					r = new BufferedReader(new InputStreamReader(is));

					// Actually do the read/write transfer loop here.
					int preambleLen = -1; // set to this for "undefined"
					while (true) {
						String inLine = r.readLine();
						if (null == inLine) {
							break; // EOF.
						}

						if (inLine.trim().length() == 0
								|| inLine.startsWith("--")) {
							continue; // comments and empty lines are ignored
						}

						if (preambleLen == -1) {
							String recordStartMark = "VALUES (";
							preambleLen = inLine.indexOf(recordStartMark)
									+ recordStartMark.length();
						}
						String s = inLine.substring(preambleLen,
								inLine.length() - 2)
								+ "\n";
						byte[] b = s.getBytes("utf-8");
						os.write(b);
					}
					out.flush();
					out.close();
				}
			}
			client.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String parse(String str) {
		// TODO Auto-generated method stub
		return str;
	}

	public static String receiveInfo(Socket sock) throws Exception // 读取服务端的反馈信息
	{
		InputStream sockIn = sock.getInputStream(); // 定义socket输入流
		byte[] bufIn = new byte[1024];
		int lenIn = sockIn.read(bufIn); // 将服务端返回的信息写入bufIn字节缓冲区
		String info = new String(bufIn, 0, lenIn);
		return info;
	}

	public static void sendInfo(Socket sock, String infoStr) throws Exception// 将信息反馈给服务端
	{
		OutputStream sockOut = sock.getOutputStream();
		sockOut.write(infoStr.getBytes("utf-8"));
	}

}