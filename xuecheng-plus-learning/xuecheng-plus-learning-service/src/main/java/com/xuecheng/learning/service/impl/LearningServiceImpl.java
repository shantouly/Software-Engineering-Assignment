package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.base.utils.StringUtil;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableItemDto;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LearningServiceImpl implements LearningService {

    @Autowired
    private MediaServiceClient mediaFeign;
    @Autowired
    private ContentServiceClient contentServiceClient;
    @Autowired
    private MyCourseTableService myCourseTableService;
    @Autowired
    private XcCourseTablesMapper xcCourseTablesMapper;

    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            XueChengPlusException.err("没有此课程相关信息");
        }

        //试学
        List<TeachPlanDto> treeNodes = contentServiceClient.getTreeNodes(courseId);
        Long id = treeNodes.get(0).getTeachPlanTreeNodes().get(0).getId();
        if (id.equals(teachplanId)) {
            return mediaFeign.getPlayUrlByMediaId(mediaId);
        }

        if (StringUtil.isNotEmpty(userId)) {
            XcCourseTablesDto learningStatus = myCourseTableService.getLearningStatus(courseId, userId);
            String learnStatus = learningStatus.getLearnStatus();
            if (learnStatus.equals("702002")) {
                return RestResponse.validfail("您尚未购买该课程");
            } else if (learnStatus.equals("702001")) {
                return mediaFeign.getPlayUrlByMediaId(mediaId);
            } else if (learnStatus.equals("702003")) {
                return RestResponse.validfail("您购买的课程已过期");
            }
        }

        String charge = coursepublish.getCharge();
        if (charge.equals("201000")) {
            return mediaFeign.getPlayUrlByMediaId(mediaId);
        }

        return RestResponse.validfail("请先购买课程");
    }


}
