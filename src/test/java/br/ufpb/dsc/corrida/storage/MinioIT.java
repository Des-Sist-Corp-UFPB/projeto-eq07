package br.ufpb.dsc.corrida.storage;

import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class MinioIT {

    @Container
    private static final MinIOContainer minioContainer = new MinIOContainer(DockerImageName.parse("minio/minio:latest"))
            .withEnv("MINIO_ACCESS_KEY", "minioadmin")
            .withEnv("MINIO_SECRET_KEY", "minioadmin");

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.endpoint-internal", minioContainer::getS3URL);
        registry.add("minio.endpoint-public", minioContainer::getS3URL);
        registry.add("minio.access-key", minioContainer::getUserName);
        registry.add("minio.secret-key", minioContainer::getPassword);
        registry.add("minio.bucket", () -> "test-bucket");
        registry.add("minio.region", () -> "sa-east-1");
    }

    @Autowired
    private MinioService minioService;

    @Autowired
    private MinioClient minioClient;

    @Test
    void fluxoCompletoDeUploadEGeracaoDeUrlEDelete() throws Exception {
        // 1. Upload
        String objectName = "fotos-perfil/teste-it.png";
        MockMultipartFile file = new MockMultipartFile("file", "teste.png", "image/png", "conteudo ficticio".getBytes());
        
        String keySalva = minioService.upload(file, objectName);
        assertEquals(objectName, keySalva);

        // Verifica se o objeto realmente existe no MinIO
        assertDoesNotThrow(() -> {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket("test-bucket")
                    .object(objectName)
                    .build());
        });

        // 2. Presigned URL
        String presignedUrl = minioService.getPresignedUrl(objectName);
        assertNotNull(presignedUrl);
        assertTrue(presignedUrl.contains(objectName));
        assertTrue(presignedUrl.contains("X-Amz-Signature"));

        // 3. Delete
        minioService.delete(objectName);
        
        // Verifica se foi apagado
        assertThrows(Exception.class, () -> {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket("test-bucket")
                    .object(objectName)
                    .build());
        });
    }
}
