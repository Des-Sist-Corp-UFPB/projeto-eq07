package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.event.RaceCompletedEvent;
import br.ufpb.dsc.corrida.exception.CorridaNaoEncontradaException;
import br.ufpb.dsc.corrida.exception.race.ConflitoHorarioException;
import br.ufpb.dsc.corrida.exception.race.CorridaCheiaException;
import br.ufpb.dsc.corrida.exception.race.InscricaoDuplicadaException;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.user.User;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final RaceRepository raceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InscricaoService(InscricaoRepository inscricaoRepository, RaceRepository raceRepository, ApplicationEventPublisher eventPublisher) {
        this.inscricaoRepository = inscricaoRepository;
        this.raceRepository = raceRepository;
        this.eventPublisher = eventPublisher;
    }

    @WithSpan("race.inscrever-atleta")
    @Transactional
    public Inscricao inscrever(User user, @SpanAttribute("race.id") Long raceId, @SpanAttribute("risk.acknowledged") boolean riskAcknowledged) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new CorridaNaoEncontradaException("Corrida não encontrada."));

        // Regra: Verifica duplicidade
        if (inscricaoRepository.existsByUsuarioAndCorridaAndStatus(user, race, StatusInscricao.ATIVA)) {
            throw new InscricaoDuplicadaException("Usuário já inscrito nesta corrida.");
        }

        // Regra: Limite de Vagas
        if (race.getMaxInscricoes() != null) {
            long currentParticipants = inscricaoRepository.countByCorridaAndStatus(race, StatusInscricao.ATIVA);
            if (currentParticipants >= race.getMaxInscricoes()) {
                throw new CorridaCheiaException("Limite de vagas atingido para esta corrida.");
            }
        }

        // Regra: Conflito de Horário
        OffsetDateTime raceStart = race.getDataInicio();
        int duracao = race.getDuracaoEstimadaMin() != null ? race.getDuracaoEstimadaMin() : 240;
        OffsetDateTime raceEnd = raceStart.plusMinutes(duracao);

        // Validar conflitos com outras inscrições ativas
        java.util.List<Inscricao> minhasInscricoes = inscricaoRepository.findByUsuarioAndStatus(user, StatusInscricao.ATIVA);
        for (Inscricao insc : minhasInscricoes) {
            if (insc.getCorrida().getId().equals(raceId)) {
                continue;
            }
            OffsetDateTime otherStart = insc.getCorrida().getDataInicio();
            if (otherStart == null) continue;
            int otherDur = insc.getCorrida().getDuracaoEstimadaMin() != null ? insc.getCorrida().getDuracaoEstimadaMin() : 240;
            OffsetDateTime otherEnd = otherStart.plusMinutes(otherDur);

            if (otherStart.isBefore(raceEnd) && otherEnd.isAfter(raceStart)) {
                throw new ConflitoHorarioException("Conflito de horário com outra corrida inscrita.");
            }
        }

        // Validar conflitos com corridas que ele organiza
        java.util.List<Race> minhasCorridasOrganizadas = raceRepository.findByOrganization_Organizer_Usuario(user);
        for (Race orgRace : minhasCorridasOrganizadas) {
            if (orgRace.getId().equals(raceId)) {
                continue;
            }
            if (orgRace.getStatus() == StatusCorrida.CANCELADA || orgRace.getStatus() == StatusCorrida.ENCERRADA) {
                continue;
            }
            OffsetDateTime otherStart = orgRace.getDataInicio();
            if (otherStart == null) continue;
            int otherDur = orgRace.getDuracaoEstimadaMin() != null ? orgRace.getDuracaoEstimadaMin() : 240;
            OffsetDateTime otherEnd = otherStart.plusMinutes(otherDur);

            if (otherStart.isBefore(raceEnd) && otherEnd.isAfter(raceStart)) {
                throw new ConflitoHorarioException("Conflito de horário com uma corrida que você organiza.");
            }
        }

        Inscricao inscricao = new Inscricao();
        inscricao.setUsuario(user);
        inscricao.setCorrida(race);
        inscricao.setStatus(StatusInscricao.ATIVA);
        inscricao.setAlertaRiscoReconhecido(riskAcknowledged);

        return inscricaoRepository.save(inscricao);
    }

    @Transactional
    public void cancelar(User user, Long inscricaoId) {
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada."));

        if (!inscricao.getUsuario().getId().equals(user.getId())) {
            throw new AcessoNaoPermitidoException("Inscrição não pertence ao usuário.");
        }

        inscricao.setStatus(StatusInscricao.CANCELADA);
        inscricaoRepository.save(inscricao);
    }

    @Transactional
    public void marcarPresenca(User organizador, Long inscricaoId, boolean presente) {
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada."));

        if (!inscricao.getCorrida().getOrganization().getOrganizer().getUsuario().getId().equals(organizador.getId())) {
            throw new AcessoNaoPermitidoException("Apenas o organizador da corrida pode confirmar presença.");
        }

        inscricao.setCompareceu(presente);
        inscricaoRepository.save(inscricao);
    }

    @Transactional
    public void processarEncerramentoCorrida(Race race) {
        // Dispara evento para atualização de KMs (desacoplado)
        eventPublisher.publishEvent(new RaceCompletedEvent(this, race));
    }
}
