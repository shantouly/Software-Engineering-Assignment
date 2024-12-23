package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MyCourseTableServiceImpl implements MyCourseTableService {
    @Autowired
    private ContentServiceClient contentServiceClient;
    @Autowired
    private XcChooseCourseMapper xcChooseCourseMapper;
    @Autowired
    private XcCourseTablesMapper xcCourseTablesMapper;


    @Override
    public XcChooseCourseDto addChooseCourse(Long courseId,String userId) {
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);

        String charge = coursepublish.getCharge();

        XcChooseCourse xcChooseCourse = null;
        if (charge.equals("201000")) {//免费课程
             xcChooseCourse = addFreeCourse(coursepublish,userId);

             XcCourseTables xcCourseTables = addCourseTables(xcChooseCourse,userId);
        } else {
            xcChooseCourse = addChargeCourse(userId,coursepublish);
        }
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(xcChooseCourse,xcChooseCourseDto);
        XcCourseTablesDto learningStatus = getLearningStatus(courseId, userId);
        xcChooseCourseDto.setLearnStatus(learningStatus.getLearnStatus());

        return xcChooseCourseDto;
    }

    @Override
    public XcCourseTablesDto getLearningStatus(Long courseId, String userId) {
        XcCourseTables courseTables = getCourseTables(userId, courseId);
        if (courseTables == null) {
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }

        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(courseTables,xcCourseTablesDto);

        boolean isValid = courseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (!isValid) {
            xcCourseTablesDto.setLearnStatus("702001");
        } else {
            xcCourseTablesDto.setLearnStatus("702003");
        }

        return xcCourseTablesDto;

    }

    @Override
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if (xcChooseCourse == null) {
            log.info("选课表查完相关记录:{}",chooseCourseId);
            return false;
        }

        String status = xcChooseCourse.getStatus();
        if (status.equals("701001")) {
            addCourseTables(xcChooseCourse,xcChooseCourse.getUserId());
            return true;
        }

        //未支付
        if (status.equals("701002")) {
            xcChooseCourse.setStatus("701001");
            int update = xcChooseCourseMapper.updateById(xcChooseCourse);
            if (update>0) {
                log.info("添加选课记录成功：{}",xcChooseCourse.getCourseId());
                addCourseTables(xcChooseCourse,xcChooseCourse.getUserId());
                return true;
            } else {
                log.info("收到支付通知处理失败，选课记录：{}",xcChooseCourse);
                return false;
            }
        }

        return false;
    }

    //添加到课程表
    private XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse, String userId) {
        //选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)){
            XueChengPlusException.err("选课未成功，无法添加到课程表");
        }
        //查询我的课程表
        XcCourseTables xcCourseTables = getCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if(xcCourseTables!=null){
            return xcCourseTables;
        }
        XcCourseTables xcCourseTablesNew = new XcCourseTables();
        xcCourseTablesNew.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTablesNew.setUserId(xcChooseCourse.getUserId());
        xcCourseTablesNew.setCourseId(xcChooseCourse.getCourseId());
        xcCourseTablesNew.setCompanyId(xcChooseCourse.getCompanyId());
        xcCourseTablesNew.setCourseName(xcChooseCourse.getCourseName());
        xcCourseTablesNew.setCreateDate(LocalDateTime.now());
        xcCourseTablesNew.setValidtimeStart(xcChooseCourse.getValidtimeStart());
        xcCourseTablesNew.setValidtimeEnd(xcChooseCourse.getValidtimeEnd());

        xcCourseTablesMapper.insert(xcCourseTablesNew);

        return xcCourseTablesNew;
    }

    public XcCourseTables getCourseTables(String userId,Long courseId){
        XcCourseTables xcCourseTables = xcCourseTablesMapper.select(userId, courseId);
        return xcCourseTables;
    }


    //添加收费课程
    public XcChooseCourse addChargeCourse(String userId,CoursePublish coursepublish) {

        //如果存在待支付记录直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002")//收费订单
                .eq(XcChooseCourse::getStatus, "701002");//待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }

        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");//收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002");//待支付

        xcChooseCourse.setValidDays(coursepublish.getValidDays());
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursepublish.getValidDays()));
        xcChooseCourseMapper.insert(xcChooseCourse);
        return xcChooseCourse;
    }
    private XcChooseCourse addFreeCourse(CoursePublish coursepublish, String userId) {
        //查询选课记录表是否存在免费的且选课成功的订单
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700001")//免费课程
                .eq(XcChooseCourse::getStatus, "701001");//选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size()>0) {
            return xcChooseCourses.get(0);
        }
        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f);//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");//选课成功

        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        xcChooseCourseMapper.insert(xcChooseCourse);

        return xcChooseCourse;
    }

    public PageResult<XcCourseTables> myCourseTables(MyCourseTableParams params) {
        int page = params.getPage();
        int size = params.getSize();

        String userId = params.getUserId();
        LambdaQueryWrapper<XcCourseTables> eq = new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId);

        Page<XcCourseTables> tablesPage = new Page<>(page, size);
        Page<XcCourseTables> pageResult = xcCourseTablesMapper.selectPage(tablesPage, eq);
        List<XcCourseTables> records = pageResult.getRecords();
        PageResult<XcCourseTables> xcCourseTablesPageResult = new PageResult<>(records, pageResult.getTotal(), page, size);
        return xcCourseTablesPageResult;
    }
}
