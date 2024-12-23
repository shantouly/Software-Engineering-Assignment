package com.xuecheng.media.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("minio_files")
public class MinioFiles {
    public String id;
    public int status;
    public LocalDateTime createtime;
    public int chunkCount;
}
