package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MediaFileProcessService {
    List<MediaProcess> getMediaFileProcess(int  shardTotal,int shardIndex,int count);

    boolean startTask(long id);

    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);

    void updateTimeOutProcess(List<Long> idList);
}
