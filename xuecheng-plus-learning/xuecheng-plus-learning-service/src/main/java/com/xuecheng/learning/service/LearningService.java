package com.xuecheng.learning.service;

import com.xuecheng.base.model.RestResponse;

/**
 * @author gushouye
 * @description 学习过程管理接口
 **/
public interface LearningService {


    /**
     * 获取视频
     *
     * @param courseId    课程id
     * @param teachplanId 课程计划id
     * @param mediaId     视频文件id
     * @param userId      用户id
     */
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId);
}
