package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.event.RaceCompletedEvent;
import br.ufpb.dsc.corrida.exception.CorridaNaoEncontradaException;
import br.ufpb.dsc.corrida.exception.race.ConflitoHorarioException;
import br.ufpb.dsc.corrida.exception.race.CorridaCheiaException;
import br.ufpb.dsc.corrida.exception.race.InscricaoDuplicadaException;
import br.ufpb.dsc.corrida.organizer.Organization;
import br.ufpb.dsc.corrida.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscricaoServiceTest {

    @Mock
    private InscricaoRepository inscricaoRepository;

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InscricaoService inscricaoService;

    private User user;
    private Race race;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        race = new Race();
        race.setId(10L);
        race.setStatus(StatusCorrida.PUBLICADA);
        race.setDataInicio(OffsetDateTime.now().plusDays(10));
        race.setDuracaoEstimadaMin(120);
        
        Organization org = new Organization();
        org.setId(1L);
        race.setOrganization(org);
    }

    @Test
    @DisplayName("inscrever() - deve realizar inscricao com sucesso")
    void inscrever_sucesso() {
        when(raceRepository.findById(10L)).thenReturn(Optional.of(race));
        when(inscricaoRepository.existsByUsuarioAndCorridaAndStatus(user, race, StatusInscricao.ATIVA)).thenReturn(false);
        when(inscricaoRepository.countOverlappingInscricoes(eq(1L), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(0L);
        when(raceRepository.countOverlappingOrganizedRaces(eq(1L), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(0L);
        
        Inscricao inscricao = new Inscricao();
        inscricao.setId(100L);
        when(inscricaoRepository.save(any(Inscricao.class))).thenReturn(inscricao);

        Inscricao result = inscricaoService.inscrever(user, 10L);

        assertNotNull(result);
        verify(inscricaoRepository).save(any(Inscricao.class));
    }

    @Test
    @DisplayName("inscrever() - deve lancar excecao de corrida cheia")
    void inscrever_corridaCheia() {
        race.setMaxInscricoes(50);
        when(raceRepository.findById(10L)).thenReturn(Optional.of(race));
        when(inscricaoRepository.existsByUsuarioAndCorridaAndStatus(user, race, StatusInscricao.ATIVA)).thenReturn(false);
        when(inscricaoRepository.countByCorridaAndStatus(race, StatusInscricao.ATIVA)).thenReturn(50L);

        assertThrows(CorridaCheiaException.class, () -> inscricaoService.inscrever(user, 10L));
    }

    @Test
    @DisplayName("inscrever() - deve lancar excecao de inscricao duplicada")
    void inscrever_duplicada() {
        when(raceRepository.findById(10L)).thenReturn(Optional.of(race));
        when(inscricaoRepository.existsByUsuarioAndCorridaAndStatus(user, race, StatusInscricao.ATIVA)).thenReturn(true);

        assertThrows(InscricaoDuplicadaException.class, () -> inscricaoService.inscrever(user, 10L));
    }

    @Test
    @DisplayName("inscrever() - deve lancar excecao de conflito de horario como corredor")
    void inscrever_conflitoHorario() {
        when(raceRepository.findById(10L)).thenReturn(Optional.of(race));
        when(inscricaoRepository.existsByUsuarioAndCorridaAndStatus(user, race, StatusInscricao.ATIVA)).thenReturn(false);
        
        when(inscricaoRepository.countOverlappingInscricoes(eq(1L), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(1L);

        assertThrows(ConflitoHorarioException.class, () -> inscricaoService.inscrever(user, 10L));
    }
    
    @Test
    @DisplayName("encerrarCorrida() - deve disparar o evento RaceCompletedEvent")
    void encerrarCorrida_sucesso() {
        race.setDistanciaKm(BigDecimal.valueOf(10.5));
        
        inscricaoService.processarEncerramentoCorrida(race);
        
        verify(eventPublisher, times(1)).publishEvent(any(RaceCompletedEvent.class));
    }
}
