package sqoop.split;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SqoopPerf {
	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();
		sqoopImport1();
		long end = System.currentTimeMillis();
		System.out.println("time cost:" + (end - start) * 1.0 / 1000 + " sec");
	}

	public static void sqoopImport1() throws Exception {
		// String cmd = "java -version";
		String cmd = "sqoop import --direct --connect jdbc:mysql://211.66.96.15:3306/weibodata --username root --password root"
				+ " --table replyrelation --target-dir /user/ping/table_operate1 --split-by userId --fields-terminated-by \"|\" --lines-terminated-by \"\\n\" --verbose";
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
	
	public static void sqoopImport2() throws Exception {
		// String cmd = "java -version";
		String cmd = "sqoop import --direct --connect jdbc:mysql://211.66.96.15:3306/weibodata --username root --password root"
				+ " --table replyrelation --target-dir /user/ping/table_operate2 --split-by userId --fields-terminated-by \"|\" --lines-terminated-by \"\\n\" --verbose";
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