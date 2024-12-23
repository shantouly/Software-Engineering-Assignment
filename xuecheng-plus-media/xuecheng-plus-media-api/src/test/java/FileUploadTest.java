


import com.xuecheng.MediaApplication;
import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaProcess;
import io.swagger.models.auth.In;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SpringBootTest(classes = MediaApplication.class)
class FileUploadTest {
    @Autowired
    private MediaProcessMapper mediaProcessMapper;
    @Test
    void testMapper() {

        List<MediaProcess> mediaProcessList = mediaProcessMapper.selectListByShardIndex(1, 0, 16);
        System.out.println(mediaProcessList);
    }

    @Test
    void testChunk() throws IOException {
        File sourceFile = new File("C:\\Users\\郭植辉\\Videos\\Captures\\saiche.mp4");
        String chunkFilePath ="C:\\Users\\郭植辉\\Videos\\Captures\\chunk\\";

        int chunkSize = 1024*1024*5;
        int chunkNum = (int) Math.ceil(sourceFile.length()/chunkSize);

        RandomAccessFile raf_r = new RandomAccessFile(sourceFile, "r");
        byte[] bytes = new byte[1024];

        for (int i = 0;i < chunkNum;i++) {
            File chunkFile = new File(chunkFilePath + i);
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len=raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);
                if (chunkFile.length() > chunkSize) {
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();
    }


    @Test
    void testMerge() throws IOException {
        File sourceFile = new File("C:\\Users\\郭植辉\\Videos\\Captures\\saiche.mp4");
        File chunkFiles = new File("C:\\Users\\郭植辉\\Videos\\Captures\\chunk");
        File mergeFile = new File("C:\\Users\\郭植辉\\Videos\\Captures\\saiche_2.mp4");

        File[] files = chunkFiles.listFiles();
        List<File> fileList = Arrays.asList(files);

        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())- Integer.parseInt(o2.getName());
            }
        });

        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        byte[] bytes = new byte[1024];

        for (File file:fileList) {
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len=raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes,0,len);
            }
            raf_r.close();
        }
        raf_rw.close();

        FileInputStream fileInputStream_merge = new FileInputStream(mergeFile);
        FileInputStream fileInputStream_source = new FileInputStream(sourceFile);
        String mergemd5 = DigestUtils.md5DigestAsHex(fileInputStream_merge);
        String sourcemd5 = DigestUtils.md5DigestAsHex(fileInputStream_source);
        if (mergemd5.equals(sourcemd5)) {
            System.out.println("文件合并成功");
        }

    }


    @Test
    void tesmp4() {
        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil("D:\\xuecheng-plus\\ffmpeg\\ffmpeg.exe", "D:\\xuecheng-plus\\video\\saiche.avi", "saiche.mp4", "D:\\xuecheng-plus\\video\\saiche.mp4");
        String s = mp4VideoUtil.generateMp4();
        System.out.println("=============>");
        System.out.println(s);

    }
}
