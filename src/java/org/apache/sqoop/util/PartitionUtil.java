package org.apache.sqoop.util;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PartitionUtil {

	public static List<Long> partition(ArrayList<Long>[] al, long numSplits,
			long minVal, long maxVal) {
		List<Long> splits = new ArrayList<Long>();
		long minSum;
		int index,indexNext;
		print(al);
		while(al[0].size()>numSplits+1){
			//找出相邻和最小的合并
			minSum = Long.MAX_VALUE;
			index=-1;indexNext=-1;
			for(int i=0;i<al[0].size()-1;i++){
				if((al[1].get(i)+al[1].get(i+1))<minSum){
					minSum=al[1].get(i)+al[1].get(i+1);
					index = i;
					indexNext = i+1;
				}
			}
			al[0].remove(indexNext);
			al[1].set(index, al[1].get(index)+al[1].get(indexNext));
			al[1].remove(indexNext);
			
			print(al);
		}
		splits.add(minVal);
		for(int i=1;i<al[0].size()-1;i++){
			splits.add(al[0].get(i));
		}
		splits.add(maxVal);
		return splits;
	}

	private static void print(ArrayList<Long>[] al) {
		for(int i=0;i<al[0].size();i++){
			System.out.print(al[0].get(i)+":"+al[1].get(i)+"    ");
		}	
		System.out.println("----------------------------------");
	}

}
