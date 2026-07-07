package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.event.RaceCompletedEvent;
import br.ufpb.dsc.corrida.exception.CorridaNaoEncontradaException;
import br.ufpb.dsc.corrida.exception.race.ConflitoHorarioException;
import br.ufpb.dsc.corrida.exception.race.CorridaCheiaException;
import br.ufpb.dsc.corrida.exception.race.InscricaoDuplicadaException;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.user.User;
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

    @Transactional
    public Inscricao inscrever(User user, Long raceId) {
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

        long inscricoesOverlapping = inscricaoRepository.countOverlappingInscricoes(user.getId(), raceStart, raceEnd);
        if (inscricoesOverlapping > 0) {
            throw new ConflitoHorarioException("Conflito de horário com outra corrida inscrita.");
        }

        // Regra: Conflito de Horário (com eventos organizados por ele)
        long organizedOverlapping = raceRepository.countOverlappingOrganizedRaces(user.getId(), raceStart, raceEnd);
        if (organizedOverlapping > 0) {
            throw new ConflitoHorarioException("Conflito de horário com uma corrida que você organiza.");
        }

        Inscricao inscricao = new Inscricao();
        inscricao.setUsuario(user);
        inscricao.setCorrida(race);
        inscricao.setStatus(StatusInscricao.ATIVA);

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
