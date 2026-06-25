package br.ufpb.dsc.corrida.storage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // Testcontainers: Sobe um container MinIO  para os testes de integração do S3
    @Container
    static MinIOContainer minioContainer = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
            .withUserName("eq07")
            .withPassword("testsecretkey");

    @Autowired
    private StorageService storageService;

    @Autowired
    private S3Client s3Client;

    // Vincula dinamicamente as portas aleatórias geradas pelo Testcontainers às propriedades do Spring
    @DynamicPropertySource
    static void s3Properties(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.endpoint", minioContainer::getS3URL);
        registry.add("aws.s3.public-endpoint", minioContainer::getS3URL);
        registry.add("aws.s3.access-key", minioContainer::getUserName);
        registry.add("aws.s3.secret-key", minioContainer::getPassword);
        registry.add("aws.s3.region", () -> "us-east-1");
        registry.add("aws.s3.bucket", () -> "eq07");
    }

    @BeforeAll
    void setupBucket() {
        // Cria o bucket "eq07" no MinIO real do container antes da execução dos testes
        s3Client.createBucket(CreateBucketRequest.builder().bucket("eq07").build());
    }

    @Test
    void testFullS3UploadDownloadAndDeleteFlow() {
        
        MultipartFile file = new MockMultipartFile("file", "test-integration.txt", "text/plain", "dados de integracao s3".getBytes());
        String generatedName = storageService.upload(file);
        
        assertNotNull(generatedName);
        assertTrue(generatedName.contains("test-integration.txt"));

        // 2. Geração da URL (Presigned URL)
        String presignedUrl = storageService.getPresignedUrl(generatedName);
        assertNotNull(presignedUrl);
        assertTrue(presignedUrl.contains(generatedName));
        assertTrue(presignedUrl.contains("eq07"));

        // 3. Deleção
        assertDoesNotThrow(() -> storageService.delete(generatedName));
    }
}
