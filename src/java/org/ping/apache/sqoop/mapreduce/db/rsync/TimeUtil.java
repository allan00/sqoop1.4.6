package org.ping.apache.sqoop.mapreduce.db.rsync;

import java.sql.Timestamp;

public class TimeUtil {
	public static String getNowStamp() {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		return getStamp(now);
	}

	public static String getStamp(Timestamp timestamp) {
		String stamp = timestamp.toString();
		stamp = stamp.replaceAll("-", "");
		stamp = stamp.replaceAll(":", "");
		stamp = stamp.replaceAll("\\.", "");
		stamp = stamp.replaceAll(" ", "");
		return stamp;
	}
}
