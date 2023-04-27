package com.xuecheng.media.service.impl;

import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.mapper.MqMessageHistoryMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author gushouye
 * @description MediaFileProcess接口实现类
 **/
@Service
@Slf4j
public class MediaFileProcessServiceImpl implements MediaFileProcessService {

    @Autowired
    private MediaProcessMapper mediaProcessMapper;

    @Autowired
    private MediaFilesMapper mediaFileMapper;

    @Autowired
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;

    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
    }

    @Override
    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result > 0;
    }

    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        // 要更新的任务
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess == null) {
            log.error("任务不存在，id:{}", taskId);
            return;
        }
        // 如果任务执行失败
        if (status.equals("3")) {
            // 更新media_process表中的状态
            mediaProcess.setStatus("3");
            mediaProcess.setFailCount(mediaProcess.getFailCount() + 1);// 失败次数+1
            mediaProcess.setErrormsg(errorMsg);
            mediaProcessMapper.updateById(mediaProcess);
            // 高校的更新方式
//            mediaProcessMapper.update(mediaProcess, null);
            return;
        }
        // 如果任务执行成功
        // 更新media_file表中的url
        // 文件表记录
        MediaFiles mediaFiles = mediaFileMapper.selectById(fileId);
        mediaFiles.setUrl(url);
        mediaFileMapper.updateById(mediaFiles);
        // 更新media_process表中的状态
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcess.setUrl(url);
        mediaProcessMapper.updateById(mediaProcess);
        // 将mediaProcess表记录插入到MediaProcessHistory表中
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess, mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        // 从media_process表中删除当前任务
        mediaProcessMapper.deleteById(taskId);

    }
}
