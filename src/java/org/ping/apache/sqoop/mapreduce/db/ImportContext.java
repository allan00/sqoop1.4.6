package org.ping.apache.sqoop.mapreduce.db;

public class ImportContext {
	private String hdfsHost = "192.168.13.150";
	private int hdfsPort = 9000;
	private String tableName = "table_operate";
	private String fileName = "mut.0";
	private String defaultPath = "/user/hadoop/"+tableName+"/";
	private String mysqlHost = "127.0.0.1";
	private int mysqlPort = 3306;
	private String DBName = "gszxglzx";
	private int deltaServerPort = 20006;
	
	public ImportContext() {
	}

	public ImportContext(String _hdfsHost, int _hdfsPort, String _tableName, String _fileName, String _defaultPath, String _mysqlHost, int _mysqlPort, String _DBName,
			int _deltaServerPort) {
		this.hdfsHost = _hdfsHost;
		this.hdfsPort = _hdfsPort;
		this.tableName = _tableName;
		this.fileName = _fileName;
		this.defaultPath = _defaultPath;
		this.mysqlHost = _mysqlHost;
		this.mysqlPort = _mysqlPort;
		this.DBName = _DBName;
		this.deltaServerPort = _deltaServerPort;
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

	public String getDefaultPath() {
		return defaultPath;
	}

	public void setDefaultPath(String defaultPath) {
		this.defaultPath = defaultPath;
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

	public String getDBName() {
		return DBName;
	}

	public void setDBName(String dBName) {
		DBName = dBName;
	}

	public int getDeltaServerPort() {
		return deltaServerPort;
	}

	public void setDeltaServerPort(int deltaServerPort) {
		this.deltaServerPort = deltaServerPort;
	}

}