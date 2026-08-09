package br.ufpb.dsc.corrida.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(minioProperties.getEndpointInternal())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .region(minioProperties.getRegion())
                .build();

        try {
            String bucket = minioProperties.getBucket();
            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket '{}' criado com sucesso no MinIO.", bucket);
            } else {
                log.info("Bucket '{}' já existe no MinIO.", bucket);
            }
        } catch (Exception e) {
            log.error("Erro ao inicializar o bucket no MinIO. Isso não impedirá o boot, mas uploads falharão até que o MinIO esteja disponível: {}", e.getMessage());
        }

        return client;
    }
}
