package br.ufpb.dsc.corrida.event;

import br.ufpb.dsc.corrida.race.Race;
import org.springframework.context.ApplicationEvent;

public class RaceCompletedEvent extends ApplicationEvent {
    
    private final Race race;

    public RaceCompletedEvent(Object source, Race race) {
        super(source);
        this.race = race;
    }

    public Race getRace() {
        return race;
    }
}
