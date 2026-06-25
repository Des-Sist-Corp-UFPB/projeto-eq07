# Relatório de Avaliação — EQ07 (DSC)

| | |
|---|---|
| **Data** | 2026-06-25 |
| **Repositório** | https://github.com/des-sist-corp-ufpb/projeto-eq07 |
| **Aplicação** | https://eq07.dsc.rodrigor.com |
| **Período de atividade** | 2026-06-23 → 2026-06-25 |
| **Total de commits** (sem merges) | 2 |
| **Integrantes** | Peterson William Da Silva Fernandes (@petersonwsf) |

---

## 1. Tecnologias

- Spring Boot 3.4.5
- Thymeleaf
- Flyway (6 migrations)
- Spring Security
- JWT
- Lombok
- Testcontainers

---

## 2. Análise Funcional

### Endpoints REST (33 mapeados)

| Método | Path | Arquivo |
|--------|------|---------|
| `POST` | `/organizacao/{orgId}/corridas` | `CorridaManagementController.java` |
| `POST` | `/organizacao/{orgId}/corridas/{id}/cancelar` | `CorridaManagementController.java` |
| `POST` | `/organizacao/{orgId}/corridas/{id}/editar` | `CorridaManagementController.java` |
| `GET` | `/corridas` | `CorridaViewController.java` |
| `GET` | `/corridas/encerradas` | `CorridaViewController.java` |
| `GET` | `/corridas/{slug}` | `CorridaViewController.java` |
| `GET` | `/organizacao/{orgId}/corridas` | `CorridaViewController.java` |
| `GET` | `/organizacao/{orgId}/corridas/nova` | `CorridaViewController.java` |
| `GET` | `/organizacao/{orgId}/corridas/{id}/editar` | `CorridaViewController.java` |
| `GET` | `/api/geo/search` | `GeocodingApiController.java` |
| `GET` | `/` | `HomeController.java` |
| `GET` | `/organizacao/{id}` | `OrganizerViewController.java` |
| `GET` | `/registrar/organizador` | `OrganizerViewController.java` |
| `POST` | `/registrar/organizador` | `OrganizerViewController.java` |
| `GET` | `/ping` | `PingController.java` |
| `DELETE` | `/user/conexao/remover/{receiverId}` | `UsuarioController.java` |
| `DELETE` | `/user/{id}` | `UsuarioController.java` |
| `GET` | `/user/userInfo/{usuarioId}` | `UsuarioController.java` |
| `GET` | `/user/{username}` | `UsuarioController.java` |
| `PATCH` | `/user/{id}` | `UsuarioController.java` |
| `POST` | `/user/conexao/aceitar/{requestId}` | `UsuarioController.java` |
| `POST` | `/user/conexao/enviar/{receiverId}` | `UsuarioController.java` |
| `POST` | `/user/conexao/recusar/{requestId}` | `UsuarioController.java` |
| `POST` | `/user/login` | `UsuarioController.java` |
| `POST` | `/user/registrar` | `UsuarioController.java` |
| `POST` | `/user/userInfo` | `UsuarioController.java` |
| `POST` | `/user/userInfo/{usuarioId}/foto-perfil` | `UsuarioController.java` |
| `PUT` | `/user/userInfo/{usuarioId}` | `UsuarioController.java` |
| `GET` | `/login` | `UsuarioViewController.java` |
| `GET` | `/minhaConta` | `UsuarioViewController.java` |
| `GET` | `/registrar` | `UsuarioViewController.java` |
| `GET` | `/solicitacoes` | `UsuarioViewController.java` |
| `GET` | `/user/{username}/profile` | `UsuarioViewController.java` |

### Entidades / Tabelas (14 encontradas)

- `organization`
- `organizer`
- `corrida`
- `user_info`
- `usuario`
- `user_connection`
- `organizer (via V5__criar_tabela_organizador.sql)`
- `organization (via V5__criar_tabela_organizador.sql)`
- `user_connection (via V4__criar_tabela_user_connection.sql)`
- `user_info (via V3__criar_tabela_user_info.sql)`
- `usuario (via V2__criar_tabela_usuario.sql)`
- `produto (via V1__criar_tabela_produto.sql)`
- `corrida (via V6__criar_tabela_corrida.sql)`
- `corrida_beneficio (via V6__criar_tabela_corrida.sql)`

### Migrations (6 arquivos)

- `V1__criar_tabela_produto.sql`
- `V2__criar_tabela_usuario.sql`
- `V3__criar_tabela_user_info.sql`
- `V4__criar_tabela_user_connection.sql`
- `V5__criar_tabela_organizador.sql`
- `V6__criar_tabela_corrida.sql`

---

## 3. Análise Arquitetural

| Aspecto | Status | Observação |
|---------|--------|-----------|
| Arquitetura em camadas | ✅ | controller=✅  service=✅  repository=✅ |
| Testes automatizados | ✅ | 18 arquivo(s) de teste |
| Migrations versionadas | ✅ | 6 migration(s) |
| Logging | ✅ | @Slf4j / LoggerFactory / logging.getLogger detectado |
| Autenticação / Segurança | ✅ | Spring Security / JWT / decorator detectado |
| DTOs / Separação de dados | ✅ | classes *DTO / *Request / *Response detectadas |
| Tratamento global de exceções | ✅ | @ControllerAdvice / @ExceptionHandler detectado |
| Documentação de API (OpenAPI) | ❌ | não detectado |
| Variáveis de ambiente | ✅ | .env / @Value / os.environ detectado |
| Dockerfile / docker-compose | ✅ | presente |

---

## 4. Contribuição por Usuário

### Resumo

| Usuário | Commits | % commits | Linhas adicionadas | Linhas no código atual | % código atual |
|---------|---------|-----------|-------------------|----------------------|----------------|
| Peterson William Da Silva Fernandes (@petersonwsf) | 2 | 100% | 12.777 | 10.780 | 100% |

### Contribuição por Camada

| Camada | Total linhas | Peterson William Da Silva Fernandes (@petersonwsf) |
|--------|-------------|---------|
| Controller | 5.759 | 100% |
| Repository | 114 | 100% |
| Service | 2.461 | 100% |
| Test | 616 | 100% |

---

## 5. Contribuição por Funcionalidade

Baseado em `git blame` nos arquivos de controller e service.

| Arquivo | Total linhas | Peterson William Da Silva Fernandes (@petersonwsf) |
|---------|-------------|---------|
| `minha-conta.html` | 472 | 100% |
| `UserControllerTest.java` | 382 | 100% |
| `CorridaServiceTest.java` | 379 | 100% |
| `index.html` | 359 | 100% |
| `UserServiceTest.java` | 357 | 100% |
| `corrida-form.html` | 329 | 100% |
| `perfil-publico.html` | 322 | 100% |
| `UserConnectionServiceTest.java` | 315 | 100% |
| `UserInfoServiceTest.java` | 297 | 100% |
| `CorridaService.java` | 288 | 100% |
| `registrar-organizador.html` | 287 | 100% |
| `corrida-detalhes.html` | 256 | 100% |
| `UserViewControllerTest.java` | 239 | 100% |
| `CorridaViewControllerTest.java` | 237 | 100% |
| `UserInfoService.java` | 234 | 100% |
| `UserInfoControllerTest.java` | 230 | 100% |
| `registrar.html` | 207 | 100% |
| `solicitacoes.html` | 206 | 100% |
| `login.html` | 198 | 100% |
| `UsuarioController.java` | 163 | 100% |
| `corridas-gerenciar.html` | 159 | 100% |
| `CorridaViewController.java` | 154 | 100% |
| `corridas-lista.html` | 154 | 100% |
| `OpenRouteServiceClient.java` | 134 | 100% |
| `UsuarioViewController.java` | 134 | 100% |
| `organizacao-detalhes.html` | 130 | 100% |
| `OrganizerServiceTest.java` | 127 | 100% |
| `UserConnectionService.java` | 125 | 100% |
| `CorridaControllerTest.java` | 120 | 100% |
| `CorridaManagementController.java` | 119 | 100% |
| `sidebar.html` | 115 | 100% |
| `UserConnectionControllerTest.java` | 105 | 100% |
| `UsuarioService.java` | 104 | 100% |
| `OrganizerControllerTest.java` | 104 | 100% |
| `OpenRouteServiceClientTest.java` | 102 | 100% |
| `OrganizerService.java` | 78 | 100% |
| `OrganizerViewController.java` | 67 | 100% |
| `TokenService.java` | 53 | 100% |
| `V6__criar_tabela_corrida.sql` | 50 | 100% |
| `CorridaApplicationTests.java` | 43 | 100% |
| `V5__criar_tabela_organizador.sql` | 34 | 100% |
| `sw.js` | 31 | 100% |
| `V3__criar_tabela_user_info.sql` | 28 | 100% |
| `CorridaApplication.java` | 25 | 100% |
| `StaticResourceConfig.java` | 25 | 100% |
| `V1__criar_tabela_produto.sql` | 25 | 100% |
| `GeocodingApiController.java` | 23 | 100% |
| `AuthService.java` | 23 | 100% |
| `PingController.java` | 20 | 100% |
| `V4__criar_tabela_user_connection.sql` | 17 | 100% |
| `ExternalServiceException.java` | 13 | 100% |
| `HomeController.java` | 13 | 100% |
| `V2__criar_tabela_usuario.sql` | 9 | 100% |

---

*Relatório gerado automaticamente em 2026-06-25.*
*Os dados de contribuição são baseados em `git log --numstat` (linhas adicionadas) e `git blame` (linhas no código atual), excluindo commits de merge.*