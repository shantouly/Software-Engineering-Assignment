import com.xuecheng.MediaApplication;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = MediaApplication.class)
public class MinioTest {
    @Autowired
    private MinioClient minioClient;
    @Test
    void uploadTest() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        for (int i = 0;i < 17;i++) {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .filename("C:\\Users\\郭植辉\\Videos\\Captures\\chunk\\" + i)
                    .object("chunk/" + i)
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("成功上传文件"+i);
        }
    }

    @Test
    void mergeTest() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        List<ComposeSource> sourceFile = new ArrayList<>();

        for (int i = 0;i < 17;i++) {
            ComposeSource composeSource = ComposeSource.builder()
                    .bucket("testbucket")
                    .object("chunk/" + i)
                    .build();
            sourceFile.add(composeSource);
        }

        ComposeObjectArgs testbucket = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merge01.mp4")
                .sources(sourceFile)
                .build();

        minioClient.composeObject(testbucket);
    }
}
