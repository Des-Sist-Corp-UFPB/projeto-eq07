# Relatório de Avaliação — EQ07 (DSC)

| | |
|---|---|
| **Data** | 2026-06-25 |
| **Repositório** | https://github.com/des-sist-corp-ufpb/projeto-eq07 |
| **Aplicação** | https://eq07.dsc.rodrigor.com |
| **Período de atividade** | 2026-06-23 → 2026-06-25 |
| **Total de commits** (sem merges, branch main) | 4 |
| **Integrantes** | Peterson William Da Silva Fernandes (@petersonwsf), Igino Medeiros De Oliveira (@IginoMedeiros) |

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

| Usuário | Commits (main) | Commits (GitHub API) | Linhas adicionadas | Linhas no código atual | % código atual |
|---------|---------------|---------------------|-------------------|----------------------|----------------|
| Peterson William Da Silva Fernandes (@petersonwsf) | 3 | **52** ⚠️ | 12.790 | 10.788 | 100% |
| Igino Medeiros De Oliveira (@IginoMedeiros) | 0 | **11** ⚠️ | 0 | 0 | 0% |
| *(sem login GitHub)* | 1 | 25% | — | — | — |

> **⚠️ Divergência entre commits locais e GitHub API:**
> - **@petersonwsf**: 3 commit(s) na branch `main` vs **52** registrados na API GitHub (commits em branches não mergeadas ou absorvidos via squash-merge sem preservação de autoria).
> - **@IginoMedeiros**: 0 commit(s) na branch `main` vs **11** registrados na API GitHub (commits em branches não mergeadas ou absorvidos via squash-merge sem preservação de autoria).
>

### Contribuição por Camada

| Camada | Total linhas | Peterson William Da Silva Fernandes (@petersonwsf) | Igino Medeiros De Oliveira (@IginoMedeiros) |
|--------|-------------|---------|---------|
| Controller | 5.767 | 100% | 0% |
| Repository | 114 | 100% | 0% |
| Service | 2.461 | 100% | 0% |
| Test | 616 | 100% | 0% |

---

## 5. Contribuição por Funcionalidade

Baseado em `git blame` nos arquivos de controller e service.

| Arquivo | Total linhas | Peterson William Da Silva Fernandes (@petersonwsf) | Igino Medeiros De Oliveira (@IginoMedeiros) |
|---------|-------------|---------|---------|
| `minha-conta.html` | 472 | 100% | 0% |
| `UserControllerTest.java` | 382 | 100% | 0% |
| `CorridaServiceTest.java` | 379 | 100% | 0% |
| `index.html` | 359 | 100% | 0% |
| `UserServiceTest.java` | 357 | 100% | 0% |
| `corrida-form.html` | 329 | 100% | 0% |
| `perfil-publico.html` | 322 | 100% | 0% |
| `UserConnectionServiceTest.java` | 315 | 100% | 0% |
| `UserInfoServiceTest.java` | 297 | 100% | 0% |
| `registrar-organizador.html` | 292 | 100% | 0% |
| `CorridaService.java` | 288 | 100% | 0% |
| `corrida-detalhes.html` | 256 | 100% | 0% |
| `UserViewControllerTest.java` | 239 | 100% | 0% |
| `CorridaViewControllerTest.java` | 237 | 100% | 0% |
| `UserInfoService.java` | 234 | 100% | 0% |
| `UserInfoControllerTest.java` | 230 | 100% | 0% |
| `registrar.html` | 207 | 100% | 0% |
| `solicitacoes.html` | 206 | 100% | 0% |
| `login.html` | 198 | 100% | 0% |
| `UsuarioController.java` | 163 | 100% | 0% |
| `corridas-gerenciar.html` | 159 | 100% | 0% |
| `CorridaViewController.java` | 154 | 100% | 0% |
| `corridas-lista.html` | 154 | 100% | 0% |
| `OpenRouteServiceClient.java` | 134 | 100% | 0% |
| `UsuarioViewController.java` | 134 | 100% | 0% |
| `organizacao-detalhes.html` | 130 | 100% | 0% |
| `OrganizerServiceTest.java` | 127 | 100% | 0% |
| `UserConnectionService.java` | 125 | 100% | 0% |
| `CorridaControllerTest.java` | 120 | 100% | 0% |
| `CorridaManagementController.java` | 119 | 100% | 0% |
| `sidebar.html` | 115 | 100% | 0% |
| `UserConnectionControllerTest.java` | 105 | 100% | 0% |
| `UsuarioService.java` | 104 | 100% | 0% |
| `OrganizerControllerTest.java` | 104 | 100% | 0% |
| `OpenRouteServiceClientTest.java` | 102 | 100% | 0% |
| `OrganizerService.java` | 78 | 100% | 0% |
| `OrganizerViewController.java` | 70 | 100% | 0% |
| `TokenService.java` | 53 | 100% | 0% |
| `V6__criar_tabela_corrida.sql` | 50 | 100% | 0% |
| `CorridaApplicationTests.java` | 43 | 100% | 0% |
| `V5__criar_tabela_organizador.sql` | 34 | 100% | 0% |
| `sw.js` | 31 | 100% | 0% |
| `V3__criar_tabela_user_info.sql` | 28 | 100% | 0% |
| `CorridaApplication.java` | 25 | 100% | 0% |
| `StaticResourceConfig.java` | 25 | 100% | 0% |
| `V1__criar_tabela_produto.sql` | 25 | 100% | 0% |
| `GeocodingApiController.java` | 23 | 100% | 0% |
| `AuthService.java` | 23 | 100% | 0% |
| `PingController.java` | 20 | 100% | 0% |
| `V4__criar_tabela_user_connection.sql` | 17 | 100% | 0% |
| `ExternalServiceException.java` | 13 | 100% | 0% |
| `HomeController.java` | 13 | 100% | 0% |
| `V2__criar_tabela_usuario.sql` | 9 | 100% | 0% |

---

*Relatório gerado automaticamente em 2026-06-25.*
*Os dados de contribuição são baseados em `git log --numstat` (linhas adicionadas) e `git blame` (linhas no código atual), excluindo commits de merge.*