package com.xuecheng.learning;

import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest

public class FeignTest {

    @Autowired
    private ContentServiceClient contentServiceClient;
    @Test
    public  void testContentFeign() {
        List<TeachPlanDto> treeNodes = contentServiceClient.getTreeNodes(28L);
        Long id = treeNodes.get(0).getId();
        System.out.println(id);
    }


}
