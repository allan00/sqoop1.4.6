package org.apache.sqoop.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.ping.apache.sqoop.mapreduce.db.rsync.Constants;

public class PartitionUtil {

	public static List<Long> partition(ArrayList<Long>[] al, long numSplits, long minVal, long maxVal) {
		List<Long> splits = new ArrayList<Long>();
		long minSum;
		int index, indexNext;
		print(al);
		while (al[0].size() > numSplits + 1) {
			// 找出相邻和最小的合并
			minSum = Long.MAX_VALUE;
			index = -1;
			indexNext = -1;
			for (int i = 0; i < al[0].size() - 1; i++) {
				if ((al[1].get(i) + al[1].get(i + 1)) < minSum) {
					minSum = al[1].get(i) + al[1].get(i + 1);
					index = i;
					indexNext = i + 1;
				}
			}
			al[0].remove(indexNext);
			al[1].set(index, al[1].get(index) + al[1].get(indexNext));
			al[1].remove(indexNext);

			print(al);
		}
		splits.add(minVal);
		for (int i = 1; i < al[0].size() - 1; i++) {
			splits.add(al[0].get(i));
		}
		splits.add(maxVal);
		return splits;
	}

	private static void print(ArrayList<Long>[] al) {
		for (int i = 0; i < al[0].size(); i++) {
			System.out.print(al[0].get(i) + ":" + al[1].get(i) + "    ");
		}
		System.out.println("----------------------------------");
	}

	public static List<Long> partition(Connection connection, String tableName, long numSplits, long minVal, long maxVal, long totalCount) throws SQLException {
		List<Long> splits = new ArrayList<Long>();
		java.sql.Statement stmt = null;
		ResultSet rs = null;
		long count = 0;
		int cursor = 0;

		try {
			long blockSize = totalCount/numSplits;
			long one = minVal,two = one+blockSize,three = two+blockSize,four = two+blockSize;
			while (splits.size() < numSplits) {
				String sql = String.format("select sum(if(id>=%d && id<%d,1,0)),sum(if(id>=%d&&id<%d,1,0)),sum(if(id>=%d&&id<%d,1,0)) from %s;",one,two,two,three,three,four,tableName);
				stmt = connection.createStatement();
				rs = stmt.executeQuery(sql);
				if (rs.next()) {
					count = rs.getLong(1);
				}
				
				if(compare(count,totalCount*cursor/numSplits) >= -Constants.THRESHOLD_PERCENT &&
						compare(count,totalCount*cursor/numSplits) <= Constants.THRESHOLD_PERCENT)	{
					splits.add(two);
					one = two;
					two = one+blockSize;
					three = two+blockSize;
					four = two+blockSize;
					continue;}
				else if(compare(count,totalCount*cursor/numSplits) < -Constants.THRESHOLD_PERCENT){
				count += rs.getLong(2);
				}
				
				
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			rs.close();
			stmt.close();
		}

		return null;
	}

	private static double compare(long count, long total) {
		long delta = count-total;
		if(delta<0)	delta = -delta;
		return delta*1.0/total;
	}

	public static List<Long> partition(ArrayList<Long>[] al, long numSplits, long minVal, long maxVal, long totalCount) {
		List<Long> splits = new ArrayList<Long>();
		long count = 0, blockSize = totalCount/numSplits;
		int cursor = 1;
		
		splits.add(minVal);
		for(int i =0;i<al[0].size();i++){
			count += al[1].get(i);
			if(count>=blockSize*cursor){
				splits.add(al[0].get(i));
				cursor++;
			}
			if(cursor == numSplits) break;
		}
		splits.add(maxVal);
		return splits;
	}

}
