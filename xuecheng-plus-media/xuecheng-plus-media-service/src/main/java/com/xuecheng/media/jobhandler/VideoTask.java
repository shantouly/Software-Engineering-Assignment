//package com.xuecheng.media.jobhandler;
//
//import com.xuecheng.base.utils.Mp4VideoUtil;
//import com.xuecheng.media.model.po.MediaProcess;
//import com.xuecheng.media.service.MediaFileProcessService;
//import com.xuecheng.media.service.MediaFileService;
//import com.xxl.job.core.context.XxlJobHelper;
//import com.xxl.job.core.handler.annotation.XxlJob;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
///**
// * description:
// * 1.xxl-job任务调度中心调度任务，按广播分片分发给不同执行器任务
// * 2.多线程，建立一个不超过cpu核心数的线程池
// * 3.为了避免多线程去抢同一个任务，这里使用乐观锁，具体在taskStart里面
// * 4.首先查询任务，任务为空就return
// * 5.把minio上的原视频下载下来
// * 6.利用ffempg工具类转码
// * 7.任务保存在mediaFileService里面当保存文件信息到数据库时，就添加了
// */
//@Slf4j
//@Component
//public class VideoTask {
//
//    @Autowired
//    private MediaFileService mediaFileService;
//    @Autowired
//    private MediaFileProcessService mediaFileProcessService;
//    @Value("${videoprocess.ffmpegpath}")
//    String ffmpegpath;
//
//    @XxlJob("videoJobHandler")
//    public void videoJobHandler() throws InterruptedException {
//        int shardTotal = XxlJobHelper.getShardTotal();
//        int shardIndex = XxlJobHelper.getShardIndex();
//        List<MediaProcess> mediaProcessList = null;
//
//        int size = 0;
//        try {
//            int processors = Runtime.getRuntime().availableProcessors();
//            mediaProcessList = mediaFileProcessService.getMediaFileProcess(shardTotal, shardIndex, processors);
//
//            size = mediaProcessList.size();
//            log.debug("取出待处理视频数：{}",size);
//            if (size <= 0) return;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return;
//        }
//
//
//        ExecutorService threadPool = Executors.newFixedThreadPool(size);
//        //计数器
//        CountDownLatch countDownLatch = new CountDownLatch(size);
//        //将处理任务放入线程池
//        mediaProcessList.forEach(mediaProcess -> {
//            threadPool.execute(() -> {
//
//                try {
//                    Long taskId = mediaProcess.getId();
//                    boolean b = mediaFileProcessService.startTask(taskId);
//                    if (!b) return;
//
//                    log.debug("开始执行任务：{}",mediaProcess);
//                    //先将文件从minio下载到服务器
//                    String bucket = mediaProcess.getBucket();
//                    String filename = mediaProcess.getFilename();
//                    String filePath = mediaProcess.getFilePath();
//                    String fileId = mediaProcess.getFileId();
//
//                    File originalFile = mediaFileService.downloadFileFromMinio(bucket, filePath);
//                    if (originalFile == null) {
//                        log.debug("下载待处理文件失败：{}",bucket.concat(filePath));
//                        mediaFileProcessService.saveProcessFinishStatus
//                                (mediaProcess.getId(),"3",fileId,null,"下载待处理文件失败");
//                        return;
//                    }
//
//                    File mp4File = null;
//
//                    try {
//                        mp4File = File.createTempFile("mp4",".mp4");
//                    } catch (IOException e) {
//                        log.error("创建临时文件失败");
//                        mediaFileProcessService.saveProcessFinishStatus
//                                (mediaProcess.getId(),"3",fileId,null,"创建临时文件失败");
//                        return;
//                    }
//
//                    //视频处理结果
//                    String result = "";
//                    try {
//                        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegpath, originalFile.getAbsolutePath(), mp4File.getName(), mp4File.getAbsolutePath());
//                        result = mp4VideoUtil.generateMp4();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        log.error("处理视频文件：{}出错：{}，",mediaProcess.getFilePath(),e.getMessage());
//                    }
//
//                    if (!result.equals("success")) {
//                        log.error("处理视频失败,视频地址：{}，错误信息：{}",bucket+filePath,result);
//                        mediaFileProcessService.saveProcessFinishStatus
//                                (mediaProcess.getId(),"3",fileId,null,result);
//                        return;
//                    }
//
//                    String objectName = getFilePath(fileId, ".mp4");
//                    String url = "/"+bucket+"/"+objectName;
//                    try {
//                        mediaFileService.addMediaFilesToMinIO
//                                (mp4File.getAbsolutePath(),"video/mp4",bucket,objectName);
//                        mediaFileProcessService.saveProcessFinishStatus
//                                (mediaProcess.getId(),"2",fileId,url,null);
//                    } catch (Exception e) {
//                        log.error("上传视频失败或者入库失败,地址视频：{}，错误信息：{}",url,e.getMessage());
//                        mediaFileProcessService.saveProcessFinishStatus
//                                (mediaProcess.getId(),"3",fileId,url,"视频上传失败或入库失败");
//                    }
//
//                } finally {
//                    countDownLatch.countDown();
//                }
//
//
//
//            });
//        });
//        //给一个超时时间，要是超时了还没完成则结束任务
//        countDownLatch.await(30, TimeUnit.MINUTES);
//    }
//
//    private String getFilePath(String fileMd5,String fileExt) {
//        return fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+fileMd5+fileExt;
//    }
//
//}


package com.xuecheng.media.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/15 11:58
 */
@Slf4j
@Component
public class VideoTask {

    @Autowired
    MediaFileService mediaFileService;
    @Autowired
    MediaFileProcessService mediaFileProcessService;


    @Value("${videoprocess.ffmpegpath}")
    String ffmpegpath;

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        List<MediaProcess> mediaProcessList = null;
        int size = 0;
        try {
            //取出cpu核心数作为一次处理数据的条数
            int processors = Runtime.getRuntime().availableProcessors();
            //一次处理视频数量不要超过cpu核心数
            mediaProcessList = mediaFileProcessService.getMediaFileProcess(shardIndex, shardTotal, processors);
            size = mediaProcessList.size();
            log.debug("取出待处理视频任务{}条", size);
            if (size <= 0) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        //启动size个线程的线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //将处理任务加入线程池
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(() -> {
                try {
                    //任务id
                    Long taskId = mediaProcess.getId();
                    //抢占任务
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        return;
                    }
                    log.debug("开始执行任务:{}", mediaProcess);
                    //下边是处理逻辑
                    //桶
                    String bucket = mediaProcess.getBucket();
                    //存储路径
                    String filePath = mediaProcess.getFilePath();
                    //原始视频的md5值
                    String fileId = mediaProcess.getFileId();
                    //原始文件名称
                    String filename = mediaProcess.getFilename();
                    //将要处理的文件下载到服务器上
                    File originalFile = mediaFileService.downloadFileFromMinio(mediaProcess.getBucket(), mediaProcess.getFilePath());
                    if (originalFile == null) {
                        log.debug("下载待处理文件失败,originalFile:{}", mediaProcess.getBucket().concat(mediaProcess.getFilePath()));
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "下载待处理文件失败");
                        return;
                    }
                    //处理下载的视频文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("mp4", ".mp4");
                    } catch (IOException e) {
                        log.error("创建mp4临时文件失败");
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "创建mp4临时文件失败");
                        return;
                    }
                    //视频处理结果
                    String result = "";
                    try {
                        //开始处理视频
                        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, originalFile.getAbsolutePath(), mp4File.getName(), mp4File.getAbsolutePath());
                        //开始视频转换，成功将返回success
                        result = videoUtil.generateMp4();
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("处理视频文件:{},出错:{}", mediaProcess.getFilePath(), e.getMessage());
                    }
//                    if (!result.equals("success")) {
//                        //记录错误信息
//                        log.error("处理视频失败,视频地址:{},错误信息:{}", bucket + filePath, result);
//                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, result);
//                        return;
//                    }

                    //将mp4上传至minio
                    //mp4在minio的存储路径
                    String objectName = getFilePath(fileId, ".mp4");
                    //访问url
                    String url = "/" + bucket + "/" + objectName;
                    try {
                        mediaFileService.addMediaFilesToMinIO(mp4File.getAbsolutePath(), "video/mp4", bucket, objectName);
                        //将url存储至数据，并更新状态为成功，并将待处理视频记录删除存入历史
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", fileId, url, null);
                    } catch (Exception e) {
                        log.error("上传视频失败或入库失败,视频地址:{},错误信息:{}", bucket + objectName, e.getMessage());
                        //最终还是失败了
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "处理后视频上传或入库失败");
                    }
                } finally {
                    countDownLatch.countDown();
                }
            });
        });
        //等待,给一个充裕的超时时间,防止无限等待，到达超时时间还没有处理完成则结束任务
        countDownLatch.await(30, TimeUnit.MINUTES);

        List<Long> idList = new ArrayList<>();
        mediaProcessList.forEach(mediaProcess -> {
            idList.add(mediaProcess.getId());
        });

        log.debug("开始补偿机制，修改超时任务状态:{}",idList);
        mediaFileProcessService.updateTimeOutProcess(idList);
    }

    private String getFilePath(String fileMd5,String fileExt){
        return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }

}