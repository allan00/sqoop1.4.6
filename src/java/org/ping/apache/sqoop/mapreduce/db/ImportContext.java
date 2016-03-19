package org.ping.apache.sqoop.mapreduce.db;

public class ImportContext {
	private String hdfsHost = "10.255.22.223";				//the ip of hdfs nameserver 
	private int hdfsPort = 9000;														//the port of hdfs
	private String tableName = "table_operate";				//the name of table to import
	private String fileName = "mut.0";										//the file name that will be in hdfs
	private String path = "/user/ping/"+tableName+"/"; //the path of file
	private String mysqlHost = "127.0.0.1";	
	private int mysqlPort = 3306;	
	private String mysqlUser = "root";
	private String mysqlPassword = "root";
	private String dbName = "gszxglzx";
	private String deltaHost = "127.0.0.1";						//the ip of the deltaServer
	private int deltaServerPort = 20006;								//the socket port to connect with deltaServer 
	
	public ImportContext() {
	}

	public ImportContext(String hdfsHost, int hdfsPort, String tableName, String fileName, String path, String mysqlHost, int mysqlPort, String mysqlUser,
			String mysqlPassword, String dbName, String deltaHost, int deltaServerPort) {
		this.hdfsHost = hdfsHost;
		this.hdfsPort = hdfsPort;
		this.tableName = tableName;
		this.fileName = fileName;
		this.path = path;
		this.mysqlHost = mysqlHost;
		this.mysqlPort = mysqlPort;
		this.mysqlUser = mysqlUser;
		this.mysqlPassword = mysqlPassword;
		this.dbName = dbName;
		this.deltaHost = deltaHost;
		this.deltaServerPort = deltaServerPort;
	}

	public String getHdfsHost() {
		return hdfsHost;
	}

	public void setHdfsHost(String hdfsHost) {
		this.hdfsHost = hdfsHost;
	}

	public int getHdfsPort() {
		return hdfsPort;
	}

	public void setHdfsPort(int hdfsPort) {
		this.hdfsPort = hdfsPort;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getMysqlHost() {
		return mysqlHost;
	}

	public void setMysqlHost(String mysqlHost) {
		this.mysqlHost = mysqlHost;
	}

	public int getMysqlPort() {
		return mysqlPort;
	}

	public void setMysqlPort(int mysqlPort) {
		this.mysqlPort = mysqlPort;
	}

	public String getMysqlUser() {
		return mysqlUser;
	}

	public void setMysqlUser(String mysqlUser) {
		this.mysqlUser = mysqlUser;
	}

	public String getMysqlPassword() {
		return mysqlPassword;
	}

	public void setMysqlPassword(String mysqlPassword) {
		this.mysqlPassword = mysqlPassword;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getDeltaHost() {
		return deltaHost;
	}

	public void setDeltaHost(String deltaHost) {
		this.deltaHost = deltaHost;
	}

	public int getDeltaServerPort() {
		return deltaServerPort;
	}

	public void setDeltaServerPort(int deltaServerPort) {
		this.deltaServerPort = deltaServerPort;
	}
		
}