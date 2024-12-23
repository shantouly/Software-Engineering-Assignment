package com.xuecheng.learning.service;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;

public interface MyCourseTableService {
    public XcChooseCourseDto addChooseCourse(Long courseId,String userId);

    public XcCourseTablesDto getLearningStatus(Long courseId, String userId);

    public boolean saveChooseCourseSuccess(String chooseCourseId);

    public PageResult<XcCourseTables> myCourseTables(MyCourseTableParams params);

}
