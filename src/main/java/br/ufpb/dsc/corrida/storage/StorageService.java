package br.ufpb.dsc.corrida.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String upload(MultipartFile file);
    String getPresignedUrl(String filename);
    void delete(String filename);
}
