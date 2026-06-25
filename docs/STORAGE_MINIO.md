# Gerenciamento de Arquivos com MinIO / AWS S3

O projeto utiliza o **MinIO** (compatível com a API do AWS S3) para armazenamento de arquivos (fotos, documentos, etc.). Isso evita salvar arquivos diretamente no banco de dados ou no disco do servidor da aplicação, facilitando a escalabilidade.

---

## 🚀 Como usar no código (Para Desenvolvedores)

Se você precisar criar uma funcionalidade que faça upload de arquivos (ex: foto de perfil do usuário, imagem de um produto), basta injetar a interface `StorageService`.

### 1. Fazendo Upload de um Arquivo

No seu Controller, receba o arquivo como `MultipartFile` e chame o método `upload()`:

```java
import br.ufpb.dsc.corrida.storage.StorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioFotoController {

    private final StorageService storageService;

    public UsuarioFotoController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/{id}/foto")
    public String uploadFoto(@RequestParam("file") MultipartFile file) {
        // O método upload salva o arquivo no MinIO e retorna o nome gerado (ex: uuid_foto.png)
        String nomeArquivoGerado = storageService.upload(file);
        
        // Salve 'nomeArquivoGerado' no banco de dados (na entidade Usuario)
        return "Arquivo salvo com sucesso: " + nomeArquivoGerado;
    }
}
```

### 2. Recuperando a URL de um Arquivo (Download/Visualização)

Para mostrar a foto no frontend, você não envia o arquivo de volta pelo Spring. Você gera uma **URL Assinada** (Presigned URL) temporária que permite o acesso direto ao MinIO:

```java
    @GetMapping("/{id}/foto-url")
    public String obterUrlFoto(@PathVariable Long id) {
        // Busque o nome do arquivo no banco de dados (ex: "550e8400-e29b-41d4-a716-446655440000_foto.png")
        String nomeArquivoSalvo = usuarioService.buscarNomeFoto(id);
        
        // Gera uma URL temporária (válida por 10 minutos)
        return storageService.getPresignedUrl(nomeArquivoSalvo);
    }
```

### 3. Deletando um Arquivo

Se o usuário excluir a conta ou trocar a foto, lembre-se de deletar o arquivo antigo do MinIO:

```java
    @DeleteMapping("/{id}/foto")
    public void deletarFoto(@PathVariable Long id) {
        String nomeArquivoSalvo = usuarioService.buscarNomeFoto(id);
        storageService.delete(nomeArquivoSalvo);
    }
```
