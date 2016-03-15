package org.ping.apache.sqoop.mapreduce.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.metastatic.rsync.Rdiff;

public class Client1 {
	public static void main(String[] args) throws Exception {
		String info = "";
		String dst = "hdfs://192.168.13.150:9000/user/ping/mut.0";
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(URI.create(dst), conf);
		FSDataInputStream mutatedStream = fs.open(new Path(dst));
		OutputStream signatureStream = new FileOutputStream(new File(
				"/tmp/mut.0.sig"));
		long start = System.currentTimeMillis();
		// client does this first
		Rdiff clientRdiff = new Rdiff();
		clientRdiff.makeSignatures(mutatedStream, signatureStream);
		// 客户端请求与本机在20006端口建立TCP连接
		Socket client = new Socket("127.0.0.1", 20006);
		// 从服务器端接收数据有个时间限制（系统自设，也可以自己设置），超过了这个时间，便会抛出该异常
		client.setSoTimeout(1000000);

		try {
			// 获取Socket的输出流，用来发送数据到服务端
			PrintStream out = new PrintStream(client.getOutputStream());
			// 获取Socket的输入流，用来接收从服务端发送过来的数据
			BufferedReader buf = new BufferedReader(new InputStreamReader(
					client.getInputStream()));
			// 发送数据到服务端
			out.println("prepare to send signature");
			info = receiveInfo(client);
			while(!"prepared to receive signature".equals(info)){
				Thread.sleep(100);
			}
			String sigFileName = "mut.0.sig";
			InputStream sigInStream = new FileInputStream(new File(
					"/tmp/mut.0.sig"));
			out.println(sigFileName);
			byte[] bufFile = new byte[1024];
			int len = 0;
			while (len != -1) {
				len = sigInStream.read(bufFile);
				out.write(bufFile, 0, len); // 将从硬盘上读取的字节数据写入socket输出流
			}

			String echo = buf.readLine();
			System.out.println(echo);
		} catch (SocketTimeoutException e) {
			System.out.println("Time out, No response");
		} finally {
			if (client != null) {
				// 如果构造函数建立起了连接，则关闭套接字，如果没有建立起连接，自然不用关闭
				client.close(); // 只关闭socket，其关联的输入输出流也会被关闭
			}
		}
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