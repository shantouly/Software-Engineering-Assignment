package com.xuecheng.media.jobhandler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.media.mapper.MinioFilesMapper;
import com.xuecheng.media.model.po.MinioFiles;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class MinioFilesTask {
    @Autowired
    private MinioFilesMapper minioFilesMapper;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MediaFileService mediaFileService;


    @XxlJob("minioJobHandler")
    public void minioJobHandler() {
        LambdaQueryWrapper<MinioFiles> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(MinioFiles::getStatus,2);
        queryWrapper.lt(MinioFiles::getCreatetime, LocalDateTime.now().minusHours(3));
        List<MinioFiles> minioFiles = minioFilesMapper.selectList(queryWrapper);
        List<String> idList = new ArrayList<>();
        minioFiles.forEach(minioFiles1 -> {
            idList.add(minioFiles1.getId());
            String path = getPath(minioFiles1.getId());
            mediaFileService.clearChunkFiles(path, minioFiles1.getChunkCount());
        });

        LambdaQueryWrapper<MinioFiles> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(MinioFiles::getId,idList);


        minioFilesMapper.delete(queryWrapper1);



    }

    private String getPath(String fileMd5) {
        return "video/"+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/chunk/";
    }
}
