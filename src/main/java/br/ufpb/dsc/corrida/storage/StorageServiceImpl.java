package br.ufpb.dsc.corrida.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
public class StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    public StorageServiceImpl(S3Client s3Client, S3Presigner s3Presigner, S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.s3Properties = s3Properties;
    }

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo não pode ser nulo ou vazio");
        }

        String originalName = file.getOriginalFilename();
        String generatedName = UUID.randomUUID().toString() + "_" + (originalName != null ? originalName : "file");

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Properties.getBucket())
                            .key(generatedName)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );
            return generatedName;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar arquivo para o S3: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPresignedUrl(String filename) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .getObjectRequest(builder -> builder
                            .bucket(s3Properties.getBucket())
                            .key(filename)
                            .build())
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar URL assinada: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(filename)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar arquivo do S3: " + e.getMessage(), e);
        }
    }
}
