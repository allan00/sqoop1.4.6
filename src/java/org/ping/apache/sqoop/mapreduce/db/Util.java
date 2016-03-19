package org.ping.apache.sqoop.mapreduce.db;

import org.metastatic.rsync.DataBlock;
import org.metastatic.rsync.Delta;
import org.metastatic.rsync.Offsets;
import org.metastatic.rsync.Rdiff;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by allan on 2016/1/22.
 */
public class Util {
    private final static int CHUNK_SIZE = Rdiff.CHUNK_SIZE;
//    public static void splitFile(File fin ,int m) throws IOException {
//        for(int i=0;i<m;i++) {
//            getFilesplit(fin, m, i);
//        }
//    }

    public static long getFilesplit(File fin,int m,int x,File fout) throws IOException {
//        File fout = new File(fin.getAbsolutePath()+".split"+x);
        FileInputStream fis = new FileInputStream(fin);
        FileOutputStream fos = new FileOutputStream(fout);
        int len;
        long count = 0;
        byte[] buf = new byte[CHUNK_SIZE];
        long mapSize = fin.length()%m ==0?(fin.length()/m):(fin.length()/m+1);
        fis.skip(x * mapSize);
        while((len=fis.read(buf))!=-1){
            if(count+len<=mapSize) {
                fos.write(buf, 0, len);
            }
            else{
                fos.write(buf,0, (int) (mapSize-count));
                //count = mapSize;
                break;
            }
            count += len;
        }
        fis.close();
        fos.close();
        return mapSize;
    }

    public static void rebuildFile(File[] srcfiles,File target) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = new FileOutputStream(target);
        byte[] buf = new byte[Rdiff.CHUNK_SIZE];
        int len;

        for(int i=0;i<srcfiles.length;i++) {
            fis = new FileInputStream(srcfiles[i]);
            while ((len = fis.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fos.flush();
        }
        fis.close();
        fos.close();
    }

    public static void mergeDelta(File[] delta,FileOutputStream deltaToal) throws IOException {
        FileInputStream fis = null;
        Rdiff rdiff = new Rdiff();
        List<Delta> totalList = new LinkedList<Delta>();
        long mapSize = delta[0].length();

        for(int i=0;i<delta.length;i++) {
            fis = new FileInputStream(delta[i]);
            List<Delta> list = rdiff.readDeltas(fis);
            Iterator it = list.iterator();
            while(it.hasNext()){
                Object o = it.next();
                if(o instanceof Offsets)
                {
                    ((Offsets) o).setNewOffset(((Offsets) o).getNewOffset()+i*mapSize);
                }
                else {
                    DataBlock temp = (DataBlock)o;
                    o = new DataBlock(temp.getOffset()+i*mapSize,temp.getData());
                }
                totalList.add((Delta)o);
            }

        }
        rdiff.writeDeltas(totalList, deltaToal);
        deltaToal.flush();
        fis.close();
        deltaToal.close();
    }

    public static boolean checkSame(File a, File b) throws IOException
    {
        FileInputStream as = new FileInputStream(a);
        FileInputStream bs = new FileInputStream(b);
        while (true)
        {
            int ai = as.read();
            int bi = bs.read();
            if (ai!=bi) {
                as.close();
                bs.close();
                return false;
            }
            if (ai == -1) {
                as.close();
                bs.close();
                return true;
            }
        }
    }
    public static void main(String args[]) throws IOException {
//        File[] farray = new File[5];
//        File src = new File("F:/gszxglzx.rar");
//        File target = new File("F:/gszxglzx_rebuild.rar");
//        for(int i=0;i<5;i++) {
//            farray[i] = getFilesplit(new File("F:/gszxglzx.rar"), 5, i);
//        }
//        rebuildFile(farray,target);
//        System.out.println("Are the two files same? --"+(checkSame(src,target)?"yes":"no"));
    }
}
