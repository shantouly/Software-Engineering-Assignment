package com.xuecheng.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.learning.model.po.XcCourseTables;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface XcCourseTablesMapper extends BaseMapper<XcCourseTables> {

    @Select("select * from xc_course_tables where user_id = #{userId} and course_id = #{courseId}")
    XcCourseTables select(@Param("userId") String userId,@Param("courseId") Long courseId);
}
