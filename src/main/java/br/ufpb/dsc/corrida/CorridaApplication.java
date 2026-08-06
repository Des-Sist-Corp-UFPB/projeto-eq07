package br.ufpb.dsc.corrida;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
    "br.ufpb.dsc.corrida.user",
    "br.ufpb.dsc.corrida.race",
    "br.ufpb.dsc.corrida.organizer",
    "br.ufpb.dsc.corrida.userConections",
    "br.ufpb.dsc.corrida.audit",
    "br.ufpb.dsc.corrida.featuretoggle",
    "br.ufpb.dsc.corrida.inscricao",
    "br.ufpb.dsc.corrida.pagamento",
})
public class CorridaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorridaApplication.class, args);
    }
}
