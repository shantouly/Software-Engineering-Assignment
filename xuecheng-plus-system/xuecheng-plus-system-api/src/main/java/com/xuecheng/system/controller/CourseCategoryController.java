package com.xuecheng.system.controller;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourseCategoryController {
    @GetMapping("/course-category/tree-nodes")
    public CourseCategoryTreeDto queryTreeNodes() {
        return null;
    }
}
