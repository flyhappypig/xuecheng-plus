package xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author gushouye
 * @description 测试大文件上传方法
 **/
public class BigFileTest {

    /**
     * 分块
     */
    @Test
    public void testChunk() throws IOException {
        // 找到源文件
        File file = new File("D:\\data1\\ry.sh");
        // 分块文件存储路径
        String chunkFilePath = "D:\\chunk";
        // 分块文件大小
        int chunkSize = 1024 * 1024 * 5;
        // 分块文件个数
        int chunkNum = (int) Math.ceil(file.length() * 1.0 / chunkSize);
        // 使用流从源文件当中读数据，向分块文件中写数据
        RandomAccessFile raf_r = new RandomAccessFile(file, "r");
        // 缓冲区
        byte[] bytes = new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
//            File.createTempFile()
            File chunkFile = new File(chunkFilePath + i);
            // 创建一个分块文件的写入流
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);
                if (chunkFile.length() >= chunkSize) {
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();
    }

    /**
     * 分块进行合并
     */
    @Test
    public void testMerge() throws IOException {
        // 找到分块文件存储路径
        File chunkFile = new File("D:\\chunk");
        // 找到源文件
        File sourceFile = new File("D:\\data1\\ry.sh");
        // 合并文件后的文件
        File mergeFile = new File("D:\\data1\\ry2.sh");
        // 取出所有分块文件
        File[] files = chunkFile.listFiles();
        // 分块文件排序
        List<File> filesList = Arrays.asList(files);
        Collections.sort(filesList, new Comparator<File>() {
            @Override
            public int compare(File t0, File t1) {
                return Integer.parseInt(t0.getName()) - Integer.parseInt(t1.getName());
            }
        });
        // 向合并文件写的流
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        // 缓冲区
        byte[] bytes = new byte[1024];
        // 遍历分块文件，向合并的文件去写
        for (File file : filesList) {
            // 读分块的流
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len = raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);
            }
            raf_r.close();
        }
        raf_rw.close();
        // 合并文件完成后，对合并的文件进行校验
        FileInputStream fileInputStream_merge = new FileInputStream(mergeFile);
        FileInputStream fileInputStream_source = new FileInputStream(sourceFile);
        String merge = DigestUtils.md5Hex(fileInputStream_merge);
        String source = DigestUtils.md5Hex(fileInputStream_source);
        if (merge.equals(source)) {
            System.out.println("文件合并完成");
        }
    }
}
