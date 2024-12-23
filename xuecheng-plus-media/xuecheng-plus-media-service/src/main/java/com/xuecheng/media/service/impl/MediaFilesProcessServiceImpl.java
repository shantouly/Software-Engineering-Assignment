package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MediaFilesProcessServiceImpl implements MediaFileProcessService {
    @Autowired
    private MediaProcessMapper mediaProcessMapper;
    @Autowired
    private MediaFilesMapper mediaFilesMapper;
    @Autowired
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;

    @Override
    public List<MediaProcess> getMediaFileProcess(int shardIndex, int shardTotal, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardIndex,shardTotal,count);
    }

    @Override
    public boolean startTask(long id) {
        int i = mediaProcessMapper.startTask(id);
        if (i <= 0) return false;
        return true;
    }

    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {

        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        //没有这条记录直接返回
        if (mediaProcess == null) return;

        LambdaQueryWrapper<MediaProcess> queryWrapper = new LambdaQueryWrapper<MediaProcess>().eq(MediaProcess::getId,taskId);
        if (status.equals("3")) {//更新任务失败
            MediaProcess mediaProcess_u = new MediaProcess();
            mediaProcess_u.setFailCount(mediaProcess.getFailCount()+1);
            mediaProcess_u.setStatus("3");
            mediaProcess_u.setErrormsg(errorMsg);
            mediaProcessMapper.update(mediaProcess_u,queryWrapper);
            log.debug("更新任务处理状态失败，任务信息：{}",mediaProcess_u);
            return;
        }

        //任务处理成功
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        if (mediaFiles != null) {
            mediaFiles.setUrl(url);
            mediaFilesMapper.updateById(mediaFiles);
        }

        mediaProcess.setUrl(url);
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcessMapper.updateById(mediaProcess);

        //添加到历史记录
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        mediaProcessMapper.deleteById(mediaProcess.getId());
    }

    @Override
    public void updateTimeOutProcess(List<Long> idList) {
        mediaProcessMapper.updateTimeOutProcess(idList);
    }
}
