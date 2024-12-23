import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaProcess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;


class FileUploadTest {
    @Autowired
    private MediaProcessMapper mediaProcessMapper;

    @Test
    void testChunk() throws IOException {
        File sourceFile = new File("C:\\Users\\郭植辉\\Videos\\Captures\\saiche.mp4");
        String chunkFilePath ="C:\\Users\\郭植辉\\Videos\\Captures\\chunk\\";

        int chunkSize = 1024*1024*1;
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
    void testMerge() {
        List<MediaProcess> mediaProcessList = mediaProcessMapper.selectListByShardIndex(1, 0, 16);
        System.out.println(mediaProcessList);
    }
}
