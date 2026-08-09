package br.ufpb.dsc.corrida.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioProperties minioProperties;

    @InjectMocks
    private MinioServiceImpl minioService;

    @BeforeEach
    void setUp() {
        lenient().when(minioProperties.getBucket()).thenReturn("test-bucket");
        lenient().when(minioProperties.getEndpointInternal()).thenReturn("http://localhost:9000");
        lenient().when(minioProperties.getEndpointPublic()).thenReturn("http://localhost:9000");
        lenient().when(minioProperties.getPresignedUrlExpirySeconds()).thenReturn(3600);
        minioService.init();
    }

    @Test
    void upload_ArquivoVazio_DeveLancarExcecao() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            minioService.upload(file, "test.jpg");
        });

        assertEquals("O arquivo não pode ser nulo ou vazio", exception.getMessage());
        verifyNoInteractions(minioClient);
    }

    @Test
    void upload_ArquivoNulo_DeveLancarExcecao() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            minioService.upload((MultipartFile) null, "test.jpg");
        });

        assertEquals("O arquivo não pode ser nulo ou vazio", exception.getMessage());
        verifyNoInteractions(minioClient);
    }

    @Test
    void upload_MimeTypeForaDaWhitelist_DeveLancarExcecao() {
        MultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            minioService.upload(file, "test.pdf");
        });

        assertTrue(exception.getMessage().contains("Tipo de arquivo não permitido"));
        verifyNoInteractions(minioClient);
    }

    @Test
    void upload_MimeTypeSemTipo_DeveLancarExcecao() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", null, "content".getBytes());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            minioService.upload(file, "test.jpg");
        });

        assertTrue(exception.getMessage().contains("Tipo de arquivo não permitido"));
        verifyNoInteractions(minioClient);
    }

    @Test
    void upload_Valido_DeveChamarMinioClient() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "content".getBytes());
        String objectName = "fotos-perfil/test.png";

        String result = minioService.upload(file, objectName);

        assertEquals(objectName, result);
        
        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        
        PutObjectArgs args = captor.getValue();
        assertEquals("test-bucket", args.bucket());
        assertEquals(objectName, args.object());
        assertEquals("image/png", args.contentType());
    }

    @Test
    void upload_StringContent_Vazio_DeveLancarExcecao() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            minioService.upload("   ", "test.json", "application/json");
        });

        assertEquals("O conteúdo não pode ser nulo ou vazio", exception.getMessage());
        verifyNoInteractions(minioClient);
    }

    @Test
    void upload_StringContent_Valido_DeveRetornarObjectName() throws Exception {
        String content = "{\"type\":\"Point\"}";
        String objectName = "rotas/route.json";

        String result = minioService.upload(content, objectName, "application/json");

        assertEquals("rotas/route.json", result);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void getPresignedUrl_Valido_DeveChamarMinioClient() throws Exception {
        String objectName = "fotos-perfil/test.jpg";
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/test-bucket/fotos-perfil/test.jpg?X-Amz-Signature=...");

        String result = minioService.getPresignedUrl(objectName);

        assertNotNull(result);
        assertTrue(result.contains(objectName));
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    void getPresignedUrl_Vazio_DeveRetornarNulo() {
        assertNull(minioService.getPresignedUrl(null));
        assertNull(minioService.getPresignedUrl("   "));
        verifyNoInteractions(minioClient);
    }

    @Test
    void delete_ObjetoValido_DeveChamarMinioClient() throws Exception {
        minioService.delete("fotos-perfil/test.jpg");

        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());
        
        RemoveObjectArgs args = captor.getValue();
        assertEquals("test-bucket", args.bucket());
        assertEquals("fotos-perfil/test.jpg", args.object());
    }

    @Test
    void delete_ObjetoVazio_NaoDeveFazerNada() {
        minioService.delete(null);
        minioService.delete("   ");
        verifyNoInteractions(minioClient);
    }

    @Test
    void delete_FalhaNoMinio_NaoDeveLancarExcecao() throws Exception {
        doThrow(new RuntimeException("Simulando falha de rede")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertDoesNotThrow(() -> {
            minioService.delete("fotos-perfil/test.jpg");
        });
        
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }
}
