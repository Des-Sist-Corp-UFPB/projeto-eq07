package br.ufpb.dsc.corrida.inscricao;

import br.ufpb.dsc.corrida.featuretoggle.FeatureToggleService;
import br.ufpb.dsc.corrida.audit.Auditable;
import br.ufpb.dsc.corrida.event.RaceCompletedEvent;
import br.ufpb.dsc.corrida.exception.CorridaNaoEncontradaException;
import br.ufpb.dsc.corrida.exception.CpfObrigatorioException;
import br.ufpb.dsc.corrida.exception.MercadoPagoException;
import br.ufpb.dsc.corrida.exception.race.ConflitoHorarioException;
import br.ufpb.dsc.corrida.exception.race.CorridaCheiaException;
import br.ufpb.dsc.corrida.exception.race.InscricaoDuplicadaException;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.pagamento.MercadoPagoService;
import br.ufpb.dsc.corrida.pagamento.Pagamento;
import br.ufpb.dsc.corrida.pagamento.PagamentoRepository;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.RaceRepository;
import br.ufpb.dsc.corrida.race.StatusCorrida;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserInfo;
import br.ufpb.dsc.corrida.user.UserInfoRepository;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final RaceRepository raceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MercadoPagoService mercadoPagoService;
    private final UserInfoRepository userInfoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final FeatureToggleService featureToggleService;

    public InscricaoService(
            InscricaoRepository inscricaoRepository,
            RaceRepository raceRepository,
            ApplicationEventPublisher eventPublisher,
            MercadoPagoService mercadoPagoService,
            UserInfoRepository userInfoRepository,
            PagamentoRepository pagamentoRepository,
            FeatureToggleService featureToggleService) {
        this.inscricaoRepository = inscricaoRepository;
        this.raceRepository = raceRepository;
        this.eventPublisher = eventPublisher;
        this.mercadoPagoService = mercadoPagoService;
        this.userInfoRepository = userInfoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.featureToggleService = featureToggleService;
    }

    @WithSpan("race.inscrever-atleta")
    @Transactional
    @Auditable(action = "RACE_ENROLLMENT", resource = "INSCRICAO", idParam = "raceId")
    public synchronized Inscricao inscrever(User user, @SpanAttribute("race.id") Long raceId,
                               @SpanAttribute("risk.acknowledged") boolean riskAcknowledged) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new CorridaNaoEncontradaException("Corrida não encontrada."));

        boolean corridaPaga = race.getValorInscricao() != null
                && race.getValorInscricao().compareTo(BigDecimal.ZERO) > 0;

        // Regra: CPF obrigatório para corridas pagas
        if (corridaPaga) {
            Optional<UserInfo> userInfoOpt = userInfoRepository.findByUsuarioId(user.getId());
            String cpf = userInfoOpt.map(UserInfo::getCpf).orElse(null);
            if (cpf == null || cpf.isBlank()) {
                log.warn("[InscricaoService] Bloqueio de inscrição: usuárioId={} sem CPF para corridaId={}",
                        user.getId(), raceId);
                throw new CpfObrigatorioException(
                        "CPF obrigatório para inscrição em corridas pagas. Atualize seu perfil.");
            }
        }

        // Regra: Verifica duplicidade (AGUARDANDO_PAGAMENTO já cobre o índice parcial do banco)
        if (inscricaoRepository.existsByUsuarioAndCorridaAndStatus(user, race, StatusInscricao.CONFIRMADA)
                || inscricaoRepository.existsByUsuarioAndCorridaAndStatus(user, race, StatusInscricao.ATIVA)
                || inscricaoRepository.existsByUsuarioAndCorridaAndStatus(user, race, StatusInscricao.AGUARDANDO_PAGAMENTO)) {
            throw new InscricaoDuplicadaException("Usuário já possui inscrição ativa nesta corrida.");
        }

        // Regra: Limite de Vagas (Opção A — conta AGUARDANDO_PAGAMENTO + ATIVA + CONFIRMADA)
        if (race.getMaxInscricoes() != null) {
            long ocupadas = inscricaoRepository.countByCorridaAndStatusIn(
                    race,
                    List.of(StatusInscricao.AGUARDANDO_PAGAMENTO,
                            StatusInscricao.ATIVA,
                            StatusInscricao.CONFIRMADA));
            if (ocupadas >= race.getMaxInscricoes()) {
                throw new CorridaCheiaException("Limite de vagas atingido para esta corrida.");
            }
        }

        // Regra: Conflito de Horário
        OffsetDateTime raceStart = race.getDataInicio();
        int duracao = race.getDuracaoEstimadaMin() != null ? race.getDuracaoEstimadaMin() : 240;
        OffsetDateTime raceEnd = raceStart.plusMinutes(duracao);

        List<Inscricao> minhasInscricoes = inscricaoRepository.findByUsuarioAndStatus(user, StatusInscricao.ATIVA);
        for (Inscricao insc : minhasInscricoes) {
            if (insc.getCorrida().getId().equals(raceId)) continue;
            OffsetDateTime otherStart = insc.getCorrida().getDataInicio();
            if (otherStart == null) continue;
            int otherDur = insc.getCorrida().getDuracaoEstimadaMin() != null
                    ? insc.getCorrida().getDuracaoEstimadaMin() : 240;
            OffsetDateTime otherEnd = otherStart.plusMinutes(otherDur);
            if (otherStart.isBefore(raceEnd) && otherEnd.isAfter(raceStart)) {
                throw new ConflitoHorarioException("Conflito de horário com outra corrida inscrita.");
            }
        }

        List<Race> minhasCorridasOrganizadas = raceRepository.findByOrganization_Organizer_Usuario(user);
        for (Race orgRace : minhasCorridasOrganizadas) {
            if (orgRace.getId().equals(raceId)) continue;
            if (orgRace.getStatus() == StatusCorrida.CANCELADA
                    || orgRace.getStatus() == StatusCorrida.ENCERRADA) continue;
            OffsetDateTime otherStart = orgRace.getDataInicio();
            if (otherStart == null) continue;
            int otherDur = orgRace.getDuracaoEstimadaMin() != null ? orgRace.getDuracaoEstimadaMin() : 240;
            OffsetDateTime otherEnd = otherStart.plusMinutes(otherDur);
            if (otherStart.isBefore(raceEnd) && otherEnd.isAfter(raceStart)) {
                throw new ConflitoHorarioException("Conflito de horário com uma corrida que você organiza.");
            }
        }

        // Cria a inscrição
        Inscricao inscricao = new Inscricao();
        inscricao.setUsuario(user);
        inscricao.setCorrida(race);
        inscricao.setAlertaRiscoReconhecido(riskAcknowledged);

        if (!corridaPaga) {
            // Corrida gratuita — confirma diretamente
            inscricao.setStatus(StatusInscricao.CONFIRMADA);
            return inscricaoRepository.save(inscricao);
        }

        // Corrida paga com PAYMENT_V2 desabilitado — confirma diretamente sem cobrança
        boolean paymentV2Enabled = featureToggleService.isFeatureEnabled("PAYMENT_V2");
        if (!paymentV2Enabled) {
            log.info("[InscricaoService] PAYMENT_V2 desabilitado — confirmando inscrição diretamente (bypass pagamento) para corridaId={}", raceId);
            inscricao.setStatus(StatusInscricao.CONFIRMADA);
            return inscricaoRepository.save(inscricao);
        }

        // Corrida paga — aguarda pagamento Pix
        inscricao.setStatus(StatusInscricao.AGUARDANDO_PAGAMENTO);
        Inscricao salva = inscricaoRepository.save(inscricao);

        try {
            mercadoPagoService.criarCobrancaPix(salva);
            log.info("[InscricaoService] Cobrança Pix criada para inscricaoId={}", salva.getId());
        } catch (MercadoPagoException e) {
            // Falha na API: cancela a inscrição para não deixar em estado inválido
            log.error("[InscricaoService] Falha ao criar Pix para inscricaoId={} — cancelando.",
                    salva.getId(), e);
            salva.setStatus(StatusInscricao.CANCELADA);
            inscricaoRepository.save(salva);
            throw e;
        }

        return salva;
    }

    @Transactional
    @Auditable(action = "CANCEL_ENROLLMENT", resource = "INSCRICAO",
               entityClass = Inscricao.class, idParam = "inscricaoId")
    public void cancelar(User user, Long inscricaoId) {
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada."));

        if (!inscricao.getUsuario().getId().equals(user.getId())) {
            throw new AcessoNaoPermitidoException("Inscrição não pertence ao usuário.");
        }

        if (inscricao.getPagamento() != null) {
            Pagamento p = inscricao.getPagamento();
            inscricao.setPagamento(null);
            pagamentoRepository.delete(p);
        }

        inscricao.setStatus(StatusInscricao.CANCELADA);
        inscricaoRepository.save(inscricao);
    }

    @Transactional
    @Auditable(action = "CONFIRM_ATTENDANCE", resource = "INSCRICAO",
               entityClass = Inscricao.class, idParam = "inscricaoId")
    public void marcarPresenca(User organizador, Long inscricaoId, boolean presente) {
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada."));

        if (!inscricao.getCorrida().getOrganization().getOrganizer().getUsuario().getId()
                .equals(organizador.getId())) {
            throw new AcessoNaoPermitidoException("Apenas o organizador da corrida pode confirmar presença.");
        }

        inscricao.setCompareceu(presente);
        inscricaoRepository.save(inscricao);
    }

    @Transactional
    public void processarEncerramentoCorrida(Race race) {
        eventPublisher.publishEvent(new RaceCompletedEvent(this, race));
    }
}
