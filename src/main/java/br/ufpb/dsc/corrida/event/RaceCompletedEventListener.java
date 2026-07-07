package br.ufpb.dsc.corrida.event;

import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.user.UserInfoRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RaceCompletedEventListener {

    private final UserInfoRepository userInfoRepository;

    public RaceCompletedEventListener(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    @EventListener
    public void handleRaceCompletedEvent(RaceCompletedEvent event) {
        Race race = event.getRace();
        if (race.getDistanciaKm() == null) {
            return;
        }

        // Bulk update na mesma transação que chamou publishEvent
        userInfoRepository.addKilometersToPresentParticipants(race.getId(), race.getDistanciaKm().floatValue());
    }
}
