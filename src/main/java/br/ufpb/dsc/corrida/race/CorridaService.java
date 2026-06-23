package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.exception.CorridaNaoEncontradaException;
import br.ufpb.dsc.corrida.exception.ExternalServiceException;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.organizer.Organization;
import br.ufpb.dsc.corrida.organizer.OrganizationRepository;
import br.ufpb.dsc.corrida.organizer.OrganizerRepository;
import br.ufpb.dsc.corrida.race.dto.CriarCorridaDTO;
import br.ufpb.dsc.corrida.race.dto.EditarCorridaDTO;
import br.ufpb.dsc.corrida.race.dto.RotaDTO;
import br.ufpb.dsc.corrida.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Serviço de negócio do módulo Corrida.
 *
 * <h2>Autorização em duas camadas</h2>
 * <ol>
 *   <li>Spring Security (SecurityConfig) garante que apenas usuários com
 *       {@code ROLE_ORGANIZER} alcançam os endpoints de gestão.</li>
 *   <li>Este serviço verifica que o organizador autenticado é o dono da
 *       organização alvo — impedindo que um organizador manipule corridas
 *       de outra organização.</li>
 * </ol>
 */
@Service
public class CorridaService {

    private static final Logger logger = LoggerFactory.getLogger(CorridaService.class);

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private OpenRouteServiceClient orsClient;

    // =========================================================================
    // Criação
    // =========================================================================

    /**
     * Cria uma nova corrida vinculada a uma organização.
     *
     * @throws AcessoNaoPermitidoException se {@code usuarioLogado} não for o
     *                                      organizador de {@code organizationId}
     * @throws ExternalServiceException     se o ORS não responder
     */
    @Transactional
    public Race criarCorrida(CriarCorridaDTO dto, Long organizationId, UserDetails usuarioLogado) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new CorridaNaoEncontradaException("Organização não encontrada"));

        verificarPropriedade(org, usuarioLogado);

        Race race = new Race();
        race.setOrganization(org);
        race.setNome(dto.nome());
        race.setDescricao(dto.descricao());
        race.setBannerUrl(dto.bannerUrl());
        race.setValorInscricao(dto.valorInscricao());
        race.setMaxInscricoes(dto.maxInscricoes());
        race.setDataInicio(dto.dataInicio());
        race.setCategoria(dto.categoria());
        race.setLargadaLat(dto.largadaLat());
        race.setLargadaLng(dto.largadaLng());
        race.setLargadaEndereco(dto.largadaEndereco());
        race.setChegadaLat(dto.chegadaLat());
        race.setChegadaLng(dto.chegadaLng());
        race.setChegadaEndereco(dto.chegadaEndereco());
        race.setBeneficios(dto.beneficios() != null ? new HashSet<>(dto.beneficios()) : new HashSet<>());
        race.setStatus(StatusCorrida.RASCUNHO);
        race.setSlug(gerarSlugUnico(dto.nome(), dto.dataInicio().getYear()));

        // Calcula rota via ORS (ExternalServiceException propaga para o controller)
        RotaDTO rota = orsClient.calcularRota(dto.largadaLng(), dto.largadaLat(),
                dto.chegadaLng(), dto.chegadaLat());
        race.setRotaGeoJson(rota.geoJson());
        race.setDistanciaKm(rota.distanciaKm());
        race.setDuracaoEstimadaMin(rota.duracaoEstimadaMin());

        return raceRepository.save(race);
    }

    // =========================================================================
    // Edição
    // =========================================================================

    /**
     * Edita uma corrida existente.
     *
     * <p>ORS só é chamado se as coordenadas mudarem — economizando cota da API.</p>
     *
     * @throws AcessoNaoPermitidoException se não for o dono
     * @throws IllegalStateException        se a corrida começa em menos de 24h
     */
    @Transactional
    public Race editarCorrida(Long id, EditarCorridaDTO dto, UserDetails usuarioLogado) {
        Race race = raceRepository.findById(id)
                .orElseThrow(() -> new CorridaNaoEncontradaException("Corrida não encontrada"));

        verificarPropriedade(race.getOrganization(), usuarioLogado);

        if (OffsetDateTime.now().isAfter(race.getDataInicio().minusHours(24))) {
            throw new IllegalStateException(
                    "Corridas só podem ser editadas com mais de 24 horas de antecedência.");
        }

        race.setNome(dto.nome());
        race.setDescricao(dto.descricao());
        race.setBannerUrl(dto.bannerUrl());
        race.setValorInscricao(dto.valorInscricao());
        race.setMaxInscricoes(dto.maxInscricoes());
        race.setDataInicio(dto.dataInicio());
        race.setCategoria(dto.categoria());
        race.setLargadaEndereco(dto.largadaEndereco());
        race.setChegadaEndereco(dto.chegadaEndereco());
        race.setBeneficios(dto.beneficios() != null ? new HashSet<>(dto.beneficios()) : new HashSet<>());

        // ORS optimization: só re-chama se as coordenadas mudaram
        boolean coordenadasMudaram =
                !Objects.equals(race.getLargadaLat(), dto.largadaLat()) ||
                !Objects.equals(race.getLargadaLng(), dto.largadaLng()) ||
                !Objects.equals(race.getChegadaLat(), dto.chegadaLat()) ||
                !Objects.equals(race.getChegadaLng(), dto.chegadaLng());

        race.setLargadaLat(dto.largadaLat());
        race.setLargadaLng(dto.largadaLng());
        race.setChegadaLat(dto.chegadaLat());
        race.setChegadaLng(dto.chegadaLng());

        if (coordenadasMudaram) {
            logger.debug("Coordenadas mudaram — re-calculando rota via ORS para corrida id={}", id);
            RotaDTO rota = orsClient.calcularRota(dto.largadaLng(), dto.largadaLat(),
                    dto.chegadaLng(), dto.chegadaLat());
            race.setRotaGeoJson(rota.geoJson());
            race.setDistanciaKm(rota.distanciaKm());
            race.setDuracaoEstimadaMin(rota.duracaoEstimadaMin());
        } else {
            logger.debug("Coordenadas inalteradas — ORS não será chamado para corrida id={}", id);
        }

        return raceRepository.save(race);
    }

    // =========================================================================
    // Cancelamento
    // =========================================================================

    @Transactional
    public void cancelarCorrida(Long id, UserDetails usuarioLogado) {
        Race race = raceRepository.findById(id)
                .orElseThrow(() -> new CorridaNaoEncontradaException("Corrida não encontrada"));

        verificarPropriedade(race.getOrganization(), usuarioLogado);
        race.setStatus(StatusCorrida.CANCELADA);
        raceRepository.save(race);
    }

    // =========================================================================
    // Consultas
    // =========================================================================

    /**
     * Feed público: apenas corridas PUBLICADA com dataInicio no futuro.
     */
    @Transactional(readOnly = true)
    public Page<Race> listarCorridas(Pageable pageable) {
        OffsetDateTime agora = OffsetDateTime.now();
        return raceRepository.findAllByStatusInAndDataInicioAfter(List.of(StatusCorrida.PUBLICADA), agora, pageable);
    }

    /**
     * Feed público com filtro de data: apenas PUBLICADA + dataInicio no futuro.
     */
    @Transactional(readOnly = true)
    public List<Race> listarCorridasPublicas() {
        OffsetDateTime agora = OffsetDateTime.now();
        return raceRepository.findAllByStatusInAndDataInicioAfter(
                        List.of(StatusCorrida.PUBLICADA), agora,Pageable.unpaged())
                .stream()
                .filter(r -> r.getDataInicio().isAfter(agora))
                .toList();
    }

    /** Histórico público: apenas ENCERRADA. */
    @Transactional(readOnly = true)
    public List<Race> listarHistorico() {
        return raceRepository.findAllByStatus(StatusCorrida.ENCERRADA);
    }

    /** Gerenciamento do organizador: todos os status, incluindo CANCELADA. */
    @Transactional(readOnly = true)
    public List<Race> listarPorOrganizacao(Long orgId) {
        return raceRepository.findAllByOrganizationId(orgId);
    }

    /**
     * Detalhe público: busca por slug; rejeita CANCELADA (trata como não existente).
     */
    @Transactional(readOnly = true)
    public Race buscarPorSlug(String slug) {
        Race race = raceRepository.findBySlug(slug)
                .orElseThrow(() -> new CorridaNaoEncontradaException("Corrida não encontrada"));
        if (race.getStatus() == StatusCorrida.CANCELADA) {
            throw new CorridaNaoEncontradaException("Corrida não encontrada");
        }
        return race;
    }

    /** Busca por ID para gerenciamento (organizador pode ver próprias corridas canceladas). */
    @Transactional(readOnly = true)
    public Race buscarPorId(Long id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> new CorridaNaoEncontradaException("Corrida não encontrada"));
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Verifica que o usuário autenticado é o organizador vinculado à organização.
     *
     * @throws AcessoNaoPermitidoException se a verificação falhar
     */
    private void verificarPropriedade(Organization org, UserDetails usuarioLogado) {
        User user = (User) usuarioLogado;
        boolean eOrganizador = organizerRepository
                .findByUsuarioId(user.getId())
                .map(organizer -> organizer.getId().equals(
                        org.getOrganizer() != null ? org.getOrganizer().getId() : null))
                .orElse(false);

        if (!eOrganizador) {
            throw new AcessoNaoPermitidoException(
                    "Você não tem permissão para gerenciar corridas desta organização.");
        }
    }

    /**
     * Gera um slug URL-safe a partir do nome e ano da corrida.
     *
     * <p>Exemplo: {@code "Meia Maratona do Sol 2026!" -> "meia-maratona-do-sol-2026"}</p>
     *
     * <p>Em caso de colisão, acrescenta sufixo numérico: {@code -2}, {@code -3}, etc.</p>
     */
    String gerarSlugUnico(String nome, int ano) {
    // Se o nome já termina com o ano, não adiciona novamente
        String nomeCompleto = nome.trim().matches(".*\\b" + ano + "\\b.*") ? nome : nome + " " + ano;

        String base = Normalizer.normalize(nomeCompleto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        String candidate = base;
        int suffix = 2;
        while (raceRepository.findBySlug(candidate).isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
