package cn.ict.zyq.bestConf.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CSVUtils {
    /**
     * 生成为CVS文件
     *
     * @param exportData 源数据List
     * @param outPutPath 文件路径
     * @param fileName   文件名称
     * @return
     */
    public File createCSVFile(List<List<String>> exportData, String outPutPath, String fileName) {
        File csvFile = null;
        BufferedWriter csvFileOutputStream = null;
        try {
            File file = new File(outPutPath);
            if (!file.exists()) {
                if (file.mkdirs()) {
                    System.out.println("创建成功");
                } else {
                    System.out.println("创建失败");
                }
            }
            //定义文件名格式并创建
            csvFile = new File(outPutPath + "/" + fileName + ".csv");
            csvFileOutputStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8), 1024);
            for (List<String> exportDatum : exportData) {
                writeRow(exportDatum, csvFileOutputStream);
                csvFileOutputStream.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (csvFileOutputStream != null) {
                    csvFileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return csvFile;
    }

    /**
     * 写一行数据
     *
     * @param row       数据列表
     * @param csvWriter
     * @throws IOException
     */
    private void writeRow(List<String> row, BufferedWriter csvWriter) throws IOException {
        int i=0;
        for (String data : row) {
            csvWriter.write(DelQuota(data));
            if (i!=row.size()-1){
                csvWriter.write(",");
            }
            i++;
        }
    }

    /**
     * 剔除特殊字符
     *
     * @param str       数据
     */
    public String DelQuota(String str) {
        String result = str;
        String[] strQuota = {"~", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "`", ";", "'", ",", ".", "/", ":", "/,", "<", ">", "?"};
        for (int i = 0; i < strQuota.length; i++) {
            if (result.indexOf(strQuota[i]) > -1)
                result = result.replace(strQuota[i], "");
        }
        return result;
    }


    /**
     *测试
     */
    public static void main(String[] args) {
        CSVUtils csvUtils=new CSVUtils();
        List<List<String>> listList=new ArrayList<List<String>>();
        List<String> list=null;
        for (int i = 0; i <5 ; i++) {
            list=new ArrayList<String>();//一个List为一行
            list.add("111");
            list.add("222");
            list.add("333");
            listList.add(list);
        }
        csvUtils.createCSVFile(listList,"/Users/minxingyang/Desktop/processed","test");
    }
}
