package org.ping.apache.sqoop.mapreduce.db.rsync;

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

public class MyPerf {
	public static final Log LOG = LogFactory.getLog(MyPerf.class.getName());
	
	public static void main(String[]  args) throws Exception {
		String hdfsHost = "master";				
		int hdfsPort = 9000;														
		String tableName = "table_operate";				
		String fileName = "mut.0";										
		String path = "/user/root/"+tableName+"/"; 
		String mysqlHost = "192.168.13.149";	
		int mysqlPort = 3306;	
		String mysqlUser = "root";
		String mysqlPassword = "root";
		String dbName = "gszxglzx";
		String deltaHost = "192.168.13.149";						
		int deltaServerPort = 20006;		
		
		ImportContext iContext = new ImportContext(hdfsHost, hdfsPort, tableName, fileName, path, mysqlHost, mysqlPort, mysqlUser, mysqlPassword, dbName, deltaHost, deltaServerPort);
		
		long start = System.currentTimeMillis();
		ImportTool.incrementalImport(iContext);
		long end = System.currentTimeMillis();
		System.out.println("time cost:"+(end-start)*1.0/1000+" sec");
	}
}