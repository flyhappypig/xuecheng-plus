package xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.j256.simplemagic.ContentType;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author gushouye
 * @description
 **/
public class MinioTest {

    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://172.16.5.102:9000/")
                    .credentials("minioadmin", "minioadmin")
                    .build();


    // 上传文件
    @Test
    public void test_upload() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        // 通过扩展名得到媒体资源类型mimeType
        ContentInfo mimeTypeMatch = ContentInfoUtil.findMimeTypeMatch(".sh");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // 通用mimeType，字节流
        if (mimeTypeMatch != null) {
            mimeType = mimeTypeMatch.getMimeType();
        }
        // 上传文件的参数信息
        UploadObjectArgs testbucket = UploadObjectArgs.builder()
                .bucket("testbucket")// 桶
                .filename("D:\\code\\workflow\\ry.sh") // 指定本地文件路径
//                .object("ry.sh")// 对象名称  在桶下存储该文件
                .object("test/ry.sh")// 对象名称 放在子目录下
                .contentType(mimeType) // 设置媒体文件类型
                .build();
        // 上传文件
        minioClient.uploadObject(testbucket);
    }

    // 删除文件
    @Test
    public void test_delete() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        // RemoveObjectArgs
        RemoveObjectArgs testbucket1 = RemoveObjectArgs
                .builder()
                .bucket("testbucket")
                .object("ry.sh")
                .build();

        minioClient.removeObject(testbucket1);
    }

    // 查询文件 从minio中下载文件
    @Test
    public void test_getFile() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        GetObjectArgs testbucket = GetObjectArgs.builder().bucket("testbucket").object("test/ry.sh").build();

        // 查询远程服务器获取到的一个流对象
        InputStream inputStream = minioClient.getObject(testbucket);
        // 指定输出流
        FileOutputStream outputStream = new FileOutputStream(new File("D:\\data1\\ry.sh"));
        // 流拷贝方法
        IOUtils.copy(inputStream, outputStream);

        // 校验文件完整性，对文件的内容进行md5
        InputStream inputStream1 = new FileInputStream("D:\\code\\workflow\\ry.sh");
        String source = DigestUtils.md5Hex(inputStream1);// 原文件的md5
        String target = DigestUtils.md5Hex(new FileInputStream(new File("D:\\data1\\ry.sh")));
//        String target = DigestUtils.md5Hex(inputStream1);
        if (source.equals(target)) {
            System.out.println("下载成功");
        }
    }

    /**
     * 将分块文件上传至minio
     */
    @Test
    public void uoloadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        for (int i = 0; i < 6; i++) {
            // 上传文件的参数信息
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbucket")// 桶
                    .filename("D:\\code\\workflow\\ry.sh" + i) // 指定本地文件路径
                    .object("chunk/" + i)// 对象名称 放在子目录下
                    .build();
            // 上传文件
            minioClient.uploadObject(testbucket);
            System.out.println("上传分块" + i + "成功");
        }
    }

    /**
     * 调用minio接口合并分块
     */
    @Test
    public void testMerge() throws Exception {
//        List<ComposeSource> sources = new ArrayList<>();
//        for (int i = 0; i < 6; i++) {
//            ComposeSource compose = ComposeSource.builder()
//                    .bucket("testBucket")
//                    .object("chunk/" + i)
//                    .build();
//            sources.add(compose);
//        }
        // 分块文件信息
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i)
                .limit(6)
                .map(i -> ComposeSource.builder()
                        .bucket("testBucket")
                        .object("chunk/" + i)
                        .build()).collect(Collectors.toList());
        // 指定合并后的objectName等信息
        ComposeObjectArgs args = ComposeObjectArgs
                .builder()
                .bucket("testbucket")
                .object("merge01.mp4")
                .sources(sources) // 指定源文件
                .build();
        // 合并文件
        // 报错，1048576 must be greater than 5242880,minio默认的分块文件大小为5M
        minioClient.composeObject(args);
    }

    /**
     * 批量清理分块文件
     */
}
