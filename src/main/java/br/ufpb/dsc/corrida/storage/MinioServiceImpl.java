package br.ufpb.dsc.corrida.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private MinioClient publicMinioClient;

    @jakarta.annotation.PostConstruct
    public void init() {
        String internal = minioProperties.getEndpointInternal();
        String publicEp = minioProperties.getEndpointPublic();
        if (internal != null && publicEp != null && !internal.equals(publicEp)) {
            log.info("Inicializando publicMinioClient para assinaturas com endpoint: {}", publicEp);
            this.publicMinioClient = MinioClient.builder()
                    .endpoint(publicEp)
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .region(minioProperties.getRegion())
                    .build();
        } else {
            this.publicMinioClient = this.minioClient;
        }
    }

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");

    @Override
    public String upload(MultipartFile file, String objectName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo não pode ser nulo ou vazio");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido. Apenas imagens (JPEG, PNG, WEBP) são aceitas.");
        }

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            log.error("Erro ao enviar arquivo para o MinIO", e);
            throw new RuntimeException("Erro ao enviar arquivo para o MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public String upload(String content, String objectName, String contentType) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("O conteúdo não pode ser nulo ou vazio");
        }

        try {
            java.io.InputStream stream = new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .stream(stream, content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, -1)
                            .contentType(contentType)
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            log.error("Erro ao enviar texto para o MinIO", e);
            throw new RuntimeException("Erro ao enviar texto para o MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPresignedUrl(String objectName) {
        if (objectName == null || objectName.trim().isEmpty()) {
            return null;
        }
        try {
            return publicMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .expiry(minioProperties.getPresignedUrlExpirySeconds(), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Erro ao gerar presigned URL para {}: {}", objectName, e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String objectName) {
        if (objectName == null || objectName.trim().isEmpty()) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Erro (ignorado) ao tentar excluir arquivo do MinIO: {}", objectName, e);
        }
    }
}
