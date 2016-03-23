package org.ping.apache.sqoop.mapreduce.db.rsync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SqoopPerf {
	public static void main(String[] args) throws Exception {
		ImportContext iContext = new ImportContext();
		long start = System.currentTimeMillis();
		sqoopImport2();
		long end = System.currentTimeMillis();
		System.out.println("time cost:" + (end - start) * 1.0 / 1000 + " sec");
	}

	// the actual function to import table incrementally
	public static void sqoopImport() throws Exception {
		// String command = "ping www.baidu.com -c 20";
		// String command =
		// "sqoop import --connect jdbc:mysql://master:3306/gszxglzx --username root"
		// +
		// " --password root --table table_operate --fields-terminated-by \"|\" --lines-terminated-by \"\\n\" --incremental lastmodified  --check-column time --merge-key id --last-value \"2008-01-01 00:00:00\" --target-dir /user/ping/table_operate2";
		String command = "sqoop import --direct --connect jdbc:mysql://db:3306/gszxglzx --username root"
				+ " --password root --table table_operate --fields-terminated-by \"|\" --lines-terminated-by \"\\n\" --incremental lastmodified  --check-column time --merge-key id --target-dir /user/ping/table_operate2";
		String[] arg = new String[]{"--last-value \"2008-01-01 00:00:00\""};
		Process p = Runtime.getRuntime().exec(command,arg);
		while (p.waitFor() != 0 && p.waitFor() != 1)
			Thread.sleep(100L);
	}

	public static void sqoopImport2() throws Exception {
		// String cmd = "java -version";
		String cmd = "sqoop import --direct --connect jdbc:mysql://db:3306/gszxglzx --username root --password root --table table_operate --fields-terminated-by \"|\" --lines-terminated-by \"\\n\" --incremental lastmodified  --check-column time --merge-key id --target-dir /user/ping/table_operate2";
		String[] arg = new String[]{"--last-value \"2008-01-01 00:00:00\""};
		Runtime rt = Runtime.getRuntime();
		Process p = null;
		int exitValue = 1;
		try {
			p = rt.exec(cmd,arg);
			InputStream is = p.getErrorStream();
			if (is == null) {
				exitValue = 1;
			}
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = "";
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
			br.close();
			isr.close();
			is.close();
			exitValue = p.waitFor();
			p.destroy();
			System.out.println(exitValue);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

}