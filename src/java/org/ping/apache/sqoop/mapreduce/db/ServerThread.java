package org.ping.apache.sqoop.mapreduce.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.metastatic.rsync.ChecksumPair;
import org.metastatic.rsync.Rdiff;

import com.ping.util.Util;

/**
 * 该类为多线程类，用于服务端
 */
public class ServerThread extends Thread {
	public static final Log LOG = LogFactory.getLog(ServerThread.class.getName());
	private Socket client = null;

	private final int MAPS = 4;
	File workDirectory = null;
	File basis = null;
	File sigFile = null;
	File[] delta = new File[MAPS];
	File deltaTotalFile = null;

	public ServerThread(Socket client) {
		this.client = client;
	}

	@Override
	public void run() {
		try {
			PrintStream out = new PrintStream(client.getOutputStream());
			InputStream in = client.getInputStream();
			String stamp = "not inited";
			String workDirectoryName = "";
			String info = "";
			String sigFileName = "";
			Process p = null;
			int len = 0;
			byte[] buf = new byte[1024 * 1024]; // 接收数据的缓存

			while (true) {
				info = receiveInfo(client);

				// 如果接受到“bye”，则退出
				if (info == null || "bye".equals(info)) {
					doClean();
					break;
				}
				// 如果接受到“bye”，则退出
				else if (info.contains("hi server,stamp:")) {
					stamp = parseStamp(info);
					workDirectoryName = System.getProperty("user.home") + "/server/" + stamp;
					workDirectory = new File(workDirectoryName);
					if (!workDirectory.exists() && !workDirectory.isDirectory()) {
						workDirectory.mkdir();
					}

					sendInfo(client, "hi client,stamp:" + stamp);
				}

				// 通过mysqldump生成数据文件
				else if (info.contains("mysqldump")) {
					// 将接收到到命令，进行处理
					String command = parse(info);
					p = Runtime.getRuntime().exec(command);
					InputStream is = p.getInputStream();
					basis = new File(workDirectoryName + "/basis");
					FileOutputStream os = new FileOutputStream(basis);
					BufferedReader r = null;
					r = new BufferedReader(new InputStreamReader(is));

					// Actually do the read/write transfer loop here.
					int preambleLen = -1; // set to this for "undefined"
					while (true) {
						String inLine = r.readLine();
						if (null == inLine) {
							break; // EOF.
						}

						if (inLine.trim().length() == 0 || inLine.startsWith("--")) {
							continue; // comments and empty lines are ignored
						}

						if (preambleLen == -1) {
							String recordStartMark = "VALUES (";
							preambleLen = inLine.indexOf(recordStartMark) + recordStartMark.length();
						}
						String s = inLine.substring(preambleLen, inLine.length() - 2) + "\n";
						byte[] b = s.getBytes("utf-8");
						os.write(b);
					}
					os.flush();
					os.close();
					is.close();
					sendInfo(client, "mysqldump finished");
				}

				// 准备接收摘要文件
				else if ("prepare to send signature".equals(info)) {
					sendInfo(client, "prepared to receive signature");
				}

				// 接收摘要文件
				else if (info.contains(".sig")) {
					sigFileName = parseFileName(info);
					long totalLen = parseLength(info);
					long count = 0;
					sendInfo(client, "ready to receive sig");
					sigFile = new File(workDirectoryName + "/" + sigFileName);

					OutputStream sigOutStream = new FileOutputStream(sigFile);

					while (count < totalLen) {
						len = in.read(buf);
						sigOutStream.write(buf, 0, len); // 写入硬盘文件
						count += len;
					}
					sigOutStream.close();
					sendInfo(client, "receive sig finished");
				}

				// 发送delta文件
				else if ("prepare to get delta".equals(info)) {
					// generate the delta of splits
					getDelta(basis, delta, sigFile);
					String deletaTotalFileName = "basis.deltatotal";
					deltaTotalFile = new File(workDirectoryName + "/" + deletaTotalFileName);

					// then merge deltas into deltaTotal
					Util.mergeDelta(delta, new FileOutputStream(deltaTotalFile));

					info = "fileName:" + deletaTotalFileName + ",length:" + deltaTotalFile.length();
					sendInfo(client, info);
					InputStream deltaInStream = new FileInputStream(deltaTotalFile);

					while (!"ready to receive delta".equals(receiveInfo(client))) {
						Thread.sleep(100L);
					}
					while ((len = deltaInStream.read(buf)) != -1) {
						out.write(buf, 0, len); // 将从硬盘上读取的字节数据写入socket输出流
					}
					deltaInStream.close();

				} else {
					Thread.sleep(100L);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void doClean() {
		if(basis.exists())	basis.delete();
		
		for (int i = 0; i < MAPS; i++) {
			if (delta[i].exists())				delta[i].delete();
		}
		
		if(sigFile.exists())		sigFile.delete();
		if(deltaTotalFile.exists())	deltaTotalFile.delete();
		if(workDirectory.exists())	workDirectory.delete();
	}

	private static String parseStamp(String info) {
		// TODO Auto-generated method stub
		int start = info.indexOf("stamp:") + 6;
		String stamp = info.substring(start);
		return stamp;
	}

	private static long parseLength(String info) {
		// TODO Auto-generated method stub
		int start = info.indexOf("length:") + 7;
		String l = info.substring(start);
		return Long.valueOf(l);
	}

	private static String parseFileName(String info) {
		int start = info.indexOf("fileName:") + 9;
		int end = info.indexOf(",length:");
		String fileName = info.substring(start, end);
		return fileName;
	}

	private void getDelta(File basis, File[] delta, File sigFile) throws FileNotFoundException, IOException, InterruptedException {
		// get the delta of splits and merge it
		final CountDownLatch endGate = new CountDownLatch(MAPS);
		File[] basisSplit = new File[MAPS];
		Rdiff serverRdiff = new Rdiff();
		List<ChecksumPair> sig = serverRdiff.readSignatures(new FileInputStream(sigFile));
		for (int i = 0; i < MAPS; i++) {
			// server gets signature file, and does this now
			new DeltaThread(basis, basisSplit, sig, delta, i, MAPS, endGate).start();
		}
		endGate.await();
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
		if (lenIn == -1)
			return "";
		String info = new String(bufIn, 0, lenIn, "utf-8");
		LOG.info("receive<----" + info);
		return info;
	}

	public static void sendInfo(Socket sock, String infoStr) throws Exception// 将信息反馈给服务端
	{
		OutputStream sockOut = sock.getOutputStream();
		sockOut.write(infoStr.getBytes("utf-8"));
		LOG.info("send---->" + infoStr);
	}

	class DeltaThread extends Thread {
		File basis = null;
		File[] basisSplit = null;
		List<ChecksumPair> sig = null;
		Rdiff serverRdiff = null;
		File[] delta = null;
		int i = 0;
		int MAPS = 0;
		CountDownLatch endGate = null;

		public DeltaThread(File basis, File[] basisSplit, List<ChecksumPair> sig, File[] delta, int i, int MAPS, CountDownLatch endGate) {
			this.basis = basis;
			this.basisSplit = basisSplit;
			this.sig = sig;
			this.delta = delta;
			serverRdiff = new Rdiff();
			this.i = i;
			this.MAPS = MAPS;
			this.endGate = endGate;
		}

		public void run() {
			try {
				basisSplit[i] = new File(basis.getAbsolutePath() + ".split" + i);
				Util.getFilesplit(basis, MAPS, i, basisSplit[i]);
				delta[i] = new File(basisSplit[i].getAbsolutePath() + ".delta");
				serverRdiff.makeDeltas(sig, new FileInputStream(basisSplit[i]), new FileOutputStream(delta[i]));
				basisSplit[i].delete();
			} catch (IOException | NoSuchAlgorithmException e) {
				e.printStackTrace();
			} finally {
				endGate.countDown();
			}
		}
	}
}