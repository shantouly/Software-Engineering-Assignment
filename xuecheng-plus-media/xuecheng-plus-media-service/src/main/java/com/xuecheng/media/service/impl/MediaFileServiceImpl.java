package com.xuecheng.media.service.impl;

import com.alibaba.nacos.common.utils.IoUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.mapper.MinioFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;

import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xuecheng.media.model.po.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
 @Service
 @Slf4j
public class MediaFileServiceImpl implements MediaFileService {

  @Autowired
  private MediaFilesMapper mediaFilesMapper;
  @Autowired
  private MediaFileService currentProxy;
  @Autowired
  private MinioClient minioClient;
  @Value("${minio.bucket.files}")
  private String bucket_files;
  @Value("${minio.bucket.videofiles}")
  private String bucket_videofiles;
  @Autowired
  private MediaProcessMapper mediaProcessMapper;
  @Autowired
  private MinioFilesMapper minioFilesMapper;

 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }

 //获取文件默认存储目录路径 年/月/日
 private String getDefaultFolderPath() {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-", "/")+"/";
  return folder;
 }

 //获取文件的md5
 private String getFileMd5(File file) {
  try (FileInputStream fileInputStream = new FileInputStream(file)) {
   String fileMd5 = DigestUtils.md5Hex(fileInputStream);
   return fileMd5;
  } catch (Exception e) {
   e.printStackTrace();
   return null;
  }
 }

 public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName) {
  try {
   UploadObjectArgs testbucket = UploadObjectArgs.builder()
           .bucket(bucket)
           .object(objectName)
           .filename(localFilePath)
           .contentType(mimeType)
           .build();
   minioClient.uploadObject(testbucket);
   log.debug("上传文件到minio成功,bucket:{},objectName:{}",bucket,objectName);
   System.out.println("上传成功");
   return true;
  } catch (Exception e) {
   e.printStackTrace();
   log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}",bucket,objectName,e.getMessage(),e);
   XueChengPlusException.err("上传文件到文件系统失败");
  }
  return false;
 }


 @Transactional
 public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName){
  //从数据库查询文件
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if (mediaFiles == null) {
   mediaFiles = new MediaFiles();
   //拷贝基本信息
   BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
   mediaFiles.setId(fileMd5);
   mediaFiles.setFileId(fileMd5);
   mediaFiles.setCompanyId(companyId);
   mediaFiles.setUrl("/" + bucket + "/" + objectName);
   mediaFiles.setBucket(bucket);
   mediaFiles.setFilePath(objectName);
   mediaFiles.setCreateDate(LocalDateTime.now());
   mediaFiles.setAuditStatus("002003");
   mediaFiles.setStatus("1");
   //保存文件信息到文件表
   int insert = mediaFilesMapper.insert(mediaFiles);
   if (insert < 0) {
    log.error("保存文件信息到数据库失败,{}",mediaFiles.toString());
    XueChengPlusException.err("保存文件信息失败");
   }
   log.debug("保存文件信息到数据库成功,{}",mediaFiles.toString());

  }

  addWaitingTask(mediaFiles);
  return mediaFiles;
 }

 private void addWaitingTask(MediaFiles mediaFiles) {
  String filename = mediaFiles.getFilename();
  String extension = filename.substring(filename.lastIndexOf("."));
  String mimeType = getMimeType(extension);

  if (mimeType.equals("video/x-msvideo")) {
   MediaProcess mediaProcess = new MediaProcess();
   BeanUtils.copyProperties(mediaFiles,mediaProcess);

   mediaProcess.setStatus("1");
   mediaProcess.setCreateDate(LocalDateTime.now());
   mediaProcess.setFailCount(0);

   mediaProcessMapper.insert(mediaProcess);
  }

 }

 private String getMimeType(String extension){
  if(extension==null)
   extension = "";
  //根据扩展名取出mimeType
  ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
  //通用mimeType，字节流
  String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
  if(extensionMatch!=null){
   mimeType = extensionMatch.getMimeType();
  }
  return mimeType;
 }


 @Override
 public RestResponse<Boolean> checkFile(String fileMd5) {
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if (mediaFiles != null) {
   String bucket = mediaFiles.getBucket();
   String filePath = mediaFiles.getFilePath();

   InputStream stream = null;

   try {
    stream = minioClient.getObject(
            GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath)
                    .build()
    );
    if (stream != null) {
     return RestResponse.success(true);
    }
   } catch (Exception e) {
   }

 }
  MinioFiles minioFiles = new MinioFiles();
  minioFiles.setId(fileMd5);
  minioFiles.setStatus(2);
  minioFiles.setCreatetime(LocalDateTime.now());
  minioFilesMapper.insert(minioFiles);

  return RestResponse.success(false);
 }


 @Override
 public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
  String chunkFilePath = chunkFileFolderPath+chunkIndex;

  InputStream fileInputStream = null;

  try {
   fileInputStream = minioClient.getObject(
           GetObjectArgs.builder()
                   .bucket(bucket_videofiles)
                   .object(chunkFilePath)
                   .build()
   );

   if (fileInputStream != null) {
    return RestResponse.success(true);
   }
  } catch (Exception e) {
  }

  return RestResponse.success(false);
 }

 @Override
 public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
  String chunkFilePath = chunkFileFolderPath+chunk;

  String mimeType = getMimeType(null);
  boolean b = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_videofiles, chunkFilePath);
  if (!b) {
   log.debug("上传分块文件失败：{}",chunkFilePath);
   return RestResponse.validfail(false,"上传文件失败");
  }
  log.debug("上传文件分块成功:{}",chunkFilePath);

  LambdaQueryWrapper<MinioFiles> queryWrapper = new LambdaQueryWrapper<>();
  MinioFiles minioFiles = minioFilesMapper.selectById(fileMd5);
  if(minioFiles.getChunkCount() == 0) {
   minioFiles.setChunkCount(1);
  } else {
   minioFiles.setChunkCount(minioFiles.getChunkCount()+1);
  }

  minioFilesMapper.updateById(minioFiles);

  return RestResponse.success(true);
 }

 private String getChunkFileFolderPath(String fileMd5) {
  return fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/chunk"+"/";
 }

 @Override
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath,String objectName) {
  File file = new File(localFilePath);
  if (!file.exists()) {
   XueChengPlusException.err("文件不存在");
  }
  //文件名称
  String filename = uploadFileParamsDto.getFilename();
  //文件扩展名
  String extension = filename.substring(filename.lastIndexOf("."));
  //文件mimeType
  String mimeType = getMimeType(extension);
  //文件的md5值
  String fileMd5 = getFileMd5(file);
  //文件的默认目录
  String defaultFolderPath = getDefaultFolderPath();
  //存储到minio中的对象名(带目录)
  if (StringUtils.isEmpty(objectName)) {
   objectName = defaultFolderPath + fileMd5 + extension;
  }
  //将文件上传到minio
  boolean b = addMediaFilesToMinIO(localFilePath, mimeType, bucket_files, objectName);
  //文件大小
  uploadFileParamsDto.setFileSize(file.length());
  //将文件信息存储到数据库
  MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_files, objectName);
  //准备返回数据
  UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
  BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
  return uploadFileResultDto;

 }

 /**
  * 合并分块文件
  * @param companyId
  * @param fileMd5
  * @param chunkTotal
  * @param uploadFileParamsDto
  * @return
  */
 @Override
 public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
  //分块文件所在文件目录
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
  //获取文件资源
  List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i)
          .limit(chunkTotal)
          .map(i -> ComposeSource.builder()
                  .bucket(bucket_videofiles)
                  .object(chunkFileFolderPath+i)
                  .build())
          .collect(Collectors.toList());

  //合并
  String filename = uploadFileParamsDto.getFilename();
  //源文件拓展名
  String extName = filename.substring(filename.lastIndexOf("."));
  //在minio中的路径
  String mergeFilePath = getFilePathByMd5(fileMd5, extName);
  //合并文件
  try {
   ObjectWriteResponse response = minioClient.composeObject(
           ComposeObjectArgs.builder()
                   .bucket(bucket_videofiles)
                   .object(mergeFilePath)
                   .sources(sourceObjectList)
                   .build()
   );
   log.debug("合并文件成功：{}",mergeFilePath);
  } catch (Exception e) {
   log.debug("合并文件失败：fileMd5:{},bucket:{},异常：{}",fileMd5,bucket_videofiles,e.getMessage());
   return RestResponse.validfail(false,"合并文件失败");
  }

  //下载文件
  File minioFile = downloadFileFromMinio(bucket_videofiles, mergeFilePath);
  //校验文件
  if (minioFile == null) {
   log.debug("下载合并文件失败：{}",mergeFilePath);
   return RestResponse.validfail("下载合并文件失败");
  }

  try (InputStream newInputStream = new FileInputStream(minioFile)) {
   String md5Hex = DigestUtils.md5Hex(newInputStream);
   if (!fileMd5.equals(md5Hex)) {
    return RestResponse.validfail(false,"合并文件校验失败:{}");
   }
   uploadFileParamsDto.setFileSize(minioFile.length());
  } catch (Exception e) {
      e.printStackTrace();
      log.debug("校验合并文件失败，fileMd5:{},文件路径：{},异常：{}",fileMd5,mergeFilePath,e.getMessage());
  } finally {
   if (minioFile != null) {
    minioFile.delete();
   }
  }

  currentProxy.addMediaFilesToDb(companyId,fileMd5,uploadFileParamsDto,bucket_videofiles,mergeFilePath);
  clearChunkFiles(chunkFileFolderPath,chunkTotal);

  MinioFiles minioFiles = minioFilesMapper.selectById(fileMd5);
  minioFiles.setStatus(1);
  minioFilesMapper.updateById(minioFiles);

  return RestResponse.success(true);
 }

 public void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {
  List<DeleteObject> deleteObjectList = Stream.iterate(0, i -> ++i)
          .limit(chunkTotal)
          .map(i -> new DeleteObject(chunkFileFolderPath+i))
          .collect(Collectors.toList());

  RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
          .bucket(bucket_videofiles)
          .objects(deleteObjectList)
          .build();

  Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
  results.forEach(r ->{

   try {
     DeleteError delErr = r.get();
   } catch (Exception e) {
    e.printStackTrace();
   }
  });
 }

 @Override
 public MediaFiles getMediaById(String mediaId) {
  MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
  return mediaFiles;
 }

 public  File downloadFileFromMinio(String bucketVideofiles, String mergeFilePath) {
   File minioFile = null;
  FileOutputStream outputStream = null;

  try {
   InputStream stream = minioClient.getObject(GetObjectArgs.builder()
           .bucket(bucket_videofiles)
           .object(mergeFilePath)
           .build());

   minioFile = File.createTempFile("minio", ".merge");
   outputStream = new FileOutputStream(minioFile);
   IoUtils.copy(stream,outputStream);
   return minioFile;
  } catch (Exception e) {
   e.printStackTrace();
  } finally {
   if (outputStream != null) {
    try {
     outputStream.close();
    } catch (IOException e) {
     throw new RuntimeException(e);
    }
   }
  }
  return null;
 }

 private String getFilePathByMd5(String fileMd5, String extName) {
    return fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+fileMd5+extName;
 }


 }
