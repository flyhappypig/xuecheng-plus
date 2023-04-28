package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 视频处理任务类
 */
@Component
@Slf4j
public class VideoTaskJob {

    @Autowired
    private MediaFileProcessService mediaFileProcessService;
    @Autowired
    private MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegPath;

    /**
     * 分片广播任务
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();// 执行器的序号 从0开始
        int shardTotal = XxlJobHelper.getShardTotal();// 执行器的总数

        // 确定CPU核数
        int cpuNum = Runtime.getRuntime().availableProcessors();
        log.info("CPU核数:{}", cpuNum);
        // 1. 获取待处理任务
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, 5);
        if (mediaProcessList == null || mediaProcessList.size() <= 0) {
            log.info("没有待处理任务");
            return;
        }
        // 任务数量
        int taskNum = mediaProcessList.size();
        log.debug("任务数量:{}", taskNum);
        if (taskNum <= 0) {
            return;
        }
        // 创建一个线程池
        ExecutorService executorService = Executors.newFixedThreadPool(cpuNum);
        // 使用计数器
        CountDownLatch countDownLatch = new CountDownLatch(taskNum);
        mediaProcessList.forEach(mediaProcess -> {
            executorService.execute(() -> {
                try {
                    // 任务id
                    Long taskId = mediaProcess.getId();
                    // 2. 获取锁，抢占任务
                    boolean result = mediaFileProcessService.startTask(taskId);
                    // 文件id
                    String fileId = mediaProcess.getFileId();
                    if (!result) {
                        log.error("抢占任务失败，任务id:{}", taskId);
                        // 保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "下载视频到本地失败");
                        return;
                    }
                    // 桶
                    String bucket = mediaProcess.getBucket();
                    // 文件objectName
                    String objectName = mediaProcess.getFilePath();
                    // 下载minio中的视频文件到本地
                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (file == null) {
                        log.error("下载视频出错，任务id:{}，bucket：{}，objectName：{}", taskId, bucket, objectName);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "创建临时文件失败");
                        return;
                    }
                    //源avi视频的路径
                    String video_path = file.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4_name = fileId + ".mp4";
                    //转换后mp4文件的路径
                    // 先创建一个临时文件，作为转换后的文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.error("创建临时文件异常，{}", e.getMessage());
                    }
                    String mp4_path = mp4File.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath, video_path, mp4_name, mp4_path);
                    // 3. 执行视频转码
                    //开始视频转换，成功将返回success,失败返回失败原因
                    String resp = videoUtil.generateMp4();
                    if (!resp.equals("success")) {
                        log.error("视频转换失败，原因：{}，bucket：{}，objectName：{}", resp, bucket, objectName);
                        // 保存任务的状态为失败
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, resp);
                        return;
                    }

                    // 4. 上传到minio
                    String mp4ObjectName = mediaProcess.getFileId() + ".mp4";
                    boolean uploadResult = mediaFileService.getUploadObjectArgs(mp4File.getAbsolutePath(), "video/mp4", bucket, mp4ObjectName);
                    if (!uploadResult) {
                        log.error("上传视频到minio出错，任务id:{}，bucket：{}，objectName：{}", taskId, bucket, mp4ObjectName);
                        return;
                    }
                    // mp4的访问路径
                    String url = getFilePath(fileId, ".mp4");
                    // 5. 更新任务状态为成功
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, null);
                } finally {
                    // 计数器减一
                    countDownLatch.countDown();
                }
            });
        });
        // 阻塞，指定最大限制的等待时间，阻塞最多等待一定的时间后就会解除堵塞
        // 指定一个时间，超过这个时间，不管任务是否执行完，都会继续执行下面的代码
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
}
