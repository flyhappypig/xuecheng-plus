package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.base.utils.StringUtil;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author gushouye
 * @description
 **/
@Service
@Slf4j
public class LearningServiceImpl implements LearningService {

    @Autowired
    private MyCourseTablesService myCourseTablesService;
    @Autowired
    private ContentServiceClient contentServiceClient;
    @Autowired
    private MediaServiceClient mediaServiceClient;

    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {
        // 查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            return RestResponse.validfail("课程不存在");
        }
        // 试学,根据课程计划的id查询课程计划信息，如果is_preview的值为1表示支持试学，否则不支持
        String teachplan = coursepublish.getTeachplan();
        TeachPlanDto teachPlanDto = JSON.parseObject(teachplan, TeachPlanDto.class);
        String isPreview = teachPlanDto.getIsPreview();
        if ("1".equals(isPreview)) {
            // 支持试学
            RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
            return playUrlByMediaId;
        }

        if (StringUtils.isNotEmpty(userId)) {
            // 通过我的课程表获取学习资格
            XcCourseTablesDto learningStatus = myCourseTablesService.getLearningStatus(userId, courseId);
            // 学习资格
            String learnStatus = learningStatus.getLearnStatus();
            if ("702002".equals(learnStatus)) {
                return RestResponse.validfail("无法学习，因为没有这课或者选课后没有支付");
            } else if ("702003".equals(learnStatus)) {
                return RestResponse.validfail("无法学习，因为已过期要申请续期或者重新支付");
            } else {
                // 有了学习资格，获取视频播放地址
                // 远程调用媒资服务获取视频播放地址
                RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
                return playUrlByMediaId;
            }
        }
        // 如果用户没有登录
        // 取出课程的收费规则
        String charge = coursepublish.getCharge();
        if (charge.equals("201000")) {
            // 免费课程，有资格学习
            // 远程调用媒资服务获取视频播放地址
            RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
            return playUrlByMediaId;
        }
        return RestResponse.validfail("该课程需要购买后才能学习");
    }
}
