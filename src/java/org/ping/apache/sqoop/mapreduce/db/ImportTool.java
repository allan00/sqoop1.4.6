package org.ping.apache.sqoop.mapreduce.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.sql.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.sqoop.mapreduce.MySQLDumpMapper;
import org.metastatic.rsync.Delta;
import org.metastatic.rsync.Rdiff;

public class ImportTool {
	public static final Log LOG = LogFactory.getLog(ImportTool.class.getName());
	
	public static void main(String[]  args) throws Exception {
		ImportContext iContext = new ImportContext();
		long start = System.currentTimeMillis();
		incrementalImport(iContext);
		long end = System.currentTimeMillis();
		System.out.println("time cost:"+(end-start)*1.0/1000+" sec");
	}
	
	
	//the actual function to import table incrementally
	public static void incrementalImport(ImportContext iContext) throws Exception{
		String fileName = iContext.getFileName();
		String realPath = iContext.getPath() + fileName;
		//dst eg: hdfs://192.168.13.150:9000/user/ping/table_operate/mut.0
		String dst = String.format("hdfs://%s:%d%s",iContext.getHdfsHost(),iContext.getHdfsPort(),realPath);
		String info = "";
		
		FileSystem fs = FileSystem.get(URI.create(dst), new Configuration());
		if (!fs.exists(new Path(realPath))) {
			fs.create(new Path(realPath));
		}
		Date now = new Date(System.currentTimeMillis());
		String stamp = TimeUtil.getNowStamp();
		FSDataInputStream mutatedInStream = fs.open(new Path(dst));
		String workDirectoryName = System.getProperty("user.home")+"/client/"+stamp;
		File workDirectory = new File(workDirectoryName);
		workDirectory.deleteOnExit();
		File mutFile = new File(workDirectoryName+"/"+fileName);
		mutFile.deleteOnExit();
		if(!workDirectory.exists()&&!workDirectory.isDirectory()){
			workDirectory.mkdir();
		}
		if(!mutFile.exists()){
			mutFile.createNewFile();
		}
		
		OutputStream mutOs = new FileOutputStream(mutFile);
		Rdiff clientRdiff = null;
		byte[] buf = new byte[1024 * 1024];
		int len = 0;
		
		//把文件下载到本地
		while ((len = mutatedInStream.read(buf)) != -1) {
			mutOs.write(buf, 0, len); 
		}
		
		Socket client = new Socket(iContext.getMysqlHost(), iContext.getDeltaServerPort());
		// 从服务器端接收数据有个时间限制（系统自设，也可以自己设置），超过了这个时间，便会抛出该异常
		client.setSoTimeout(1000000);
		InputStream in = client.getInputStream();
		OutputStream out = client.getOutputStream();
		try {
			//建立连接后，发送stamp
			info="hi server,stamp:"+stamp;
			sendInfo(client,info);
			while (!("hi client,stamp:"+stamp).equals(receiveInfo(client))) {
				Thread.sleep(100L);
			}
			
			// 先让deltaServer用mysqldump把数据导出
			info = String.format("mysqldump --host=%s --port=%s --skip-opt --compact --no-create-db --no-create-info --quick"
					+ " --single-transaction -u%s -p%s %s %s",iContext.getMysqlHost(),iContext.getMysqlPort(), iContext.getMysqlUser(),iContext.getMysqlPassword(),iContext.getDbName(),iContext.getTableName());
			sendInfo(client,info);
			while (!"mysqldump finished".equals(receiveInfo(client))) {
				Thread.sleep(100L);
			}
			
			sendInfo(client, "prepare to send signature");
			while (!"prepared to receive signature".equals(receiveInfo(client))) {
				Thread.sleep(100L);
			}
			
			//生成文件的摘要(signature)
			String sigFileName = fileName + ".sig";
			File sigFile  =new File(workDirectoryName+"/" + sigFileName);
			sigFile.deleteOnExit();
			OutputStream signOutStream = null;
			signOutStream = new FileOutputStream(sigFile);
			clientRdiff = new Rdiff();
			clientRdiff.makeSignatures(new FileInputStream(mutFile), signOutStream);
			
			mutOs.close();
			mutatedInStream.close();
			signOutStream.close();
			
		
			//发送摘要文件到deltaServer,以供deltaServer生成delta文件
			InputStream sigInStream = new FileInputStream(sigFile);
			info = "fileName:" + sigFileName + ",length:" + sigFile.length();
			sendInfo(client, info);
			
			while (!"ready to receive sig".equals(receiveInfo(client))) {
				Thread.sleep(100L);
			}
			while ((len = sigInStream.read(buf)) != -1) {
				out.write(buf, 0, len);
			}
			sigInStream.close();
			while (!"receive sig finished".equals(receiveInfo(client))) {
				Thread.sleep(100L);
			}

			//请求接收delta文件
			sendInfo(client, "prepare to get delta");
			while (!(info = receiveInfo(client)).contains(".deltatotal")) {
				Thread.sleep(100L);
			}
			
			//接收delta文件
			String deltaFileName = parseFileName(info);
			File deltaFile = new File(workDirectoryName+"/" + deltaFileName);
			deltaFile.deleteOnExit();
			long totalLen = parseLength(info);
			long count = 0;
			FileOutputStream deltaOutStream = new FileOutputStream(deltaFile);
			sendInfo(client, "ready to receive delta");
			while (count < totalLen) {
				len = in.read(buf);
				deltaOutStream.write(buf, 0, len); // 写入硬盘文件
				count += len;
			}
			deltaOutStream.close();
			sendInfo(client, "bye");
			client.close();
			
			//如delta文件不为空，则利用delta和原文件重构文件，并上传到hdfs
			if(deltaFile.length()!=0){			
			String outputFileName = fileName+".output";
			Path outpath = new Path(iContext.getPath() +"/"+ outputFileName);
			FSDataOutputStream outputStream = fs.create(outpath); // 创建文件
			List<Delta> delta = clientRdiff.readDeltas(new FileInputStream(deltaFile));
			clientRdiff.rebuildFile(mutFile, delta, outputStream);
			fs.delete(new Path(realPath));
			fs.rename(outpath, new Path(realPath));
			}
		} catch (SocketTimeoutException e) {
			System.out.println("Time out, No response");
		} finally {
			if (client != null) {
				// 如果构造函数建立起了连接，则关闭套接字，如果没有建立起连接，自然不用关闭
				client.close(); // 只关闭socket，其关联的输入输出流也会被关闭
			}
		}
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
}