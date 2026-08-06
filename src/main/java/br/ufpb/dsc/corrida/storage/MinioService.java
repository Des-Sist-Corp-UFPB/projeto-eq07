package br.ufpb.dsc.corrida.storage;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {
    String upload(MultipartFile file, String objectName);
    String upload(String content, String objectName, String contentType);
    String getPresignedUrl(String objectName);
    void delete(String objectName);
}
