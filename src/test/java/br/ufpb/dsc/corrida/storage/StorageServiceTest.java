package br.ufpb.dsc.corrida.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Properties s3Properties;

    @InjectMocks
    private StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        lenient().when(s3Properties.getBucket()).thenReturn("eq07");
    }

    @Test
    void testUpload_ValidFile_CallsS3ClientAndReturnsGeneratedName() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "test data".getBytes());
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String generatedName = storageService.upload(file);

        assertNotNull(generatedName);
        assertTrue(generatedName.contains("test.png"));
        // O UUID tem 36 caracteres, o nome gerado deve ser UUID_test.png
        assertTrue(generatedName.length() > 36);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("eq07", request.bucket());
        assertEquals(generatedName, request.key());
        assertEquals("image/png", request.contentType());
    }

    @Test
    void testUpload_EmptyFile_ThrowsException() {
        MultipartFile emptyFile = new MockMultipartFile("file", "", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> {
            storageService.upload(emptyFile);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            storageService.upload(null);
        });
    }

    @Test
    void testGetPresignedUrl_CallsS3PresignerAndReturnsUrl() throws Exception {
        String filename = "some-file-uuid.png";
        URL mockUrl = new URL("https://s3.dsc.rodrigor.com/eq07/" + filename + "?signature=abc");
        
        PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);
        when(mockPresignedRequest.url()).thenReturn(mockUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedRequest);

        String presignedUrl = storageService.getPresignedUrl(filename);

        assertEquals(mockUrl.toString(), presignedUrl);
        
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        
        GetObjectPresignRequest request = captor.getValue();
        assertEquals("eq07", request.getObjectRequest().bucket());
        assertEquals(filename, request.getObjectRequest().key());
    }

    @Test
    void testDelete_CallsS3Client() {
        String filename = "file-to-delete.png";
        
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        storageService.delete(filename);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest request = captor.getValue();
        assertEquals("eq07", request.bucket());
        assertEquals(filename, request.key());
    }
}
