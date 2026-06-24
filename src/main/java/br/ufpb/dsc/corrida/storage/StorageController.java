package br.ufpb.dsc.corrida.storage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        String filename = storageService.upload(file);
        return ResponseEntity.ok(filename);
    }

    @GetMapping("/download-url/{filename}")
    public ResponseEntity<String> getDownloadUrl(@PathVariable String filename) {
        String presignedUrl = storageService.getPresignedUrl(filename);
        return ResponseEntity.ok(presignedUrl);
    }
}
