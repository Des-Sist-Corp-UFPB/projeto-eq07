package br.ufpb.dsc.corrida.config.security;
import br.ufpb.dsc.corrida.user.User;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String criarToken(User usuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("corrida_api")
                    .withSubject(usuario.getLogin())
                    .withClaim("id", usuario.getId())
                    .withClaim("nome", usuario.getNome())
                    .withClaim("papel", usuario.getPapel().name())
                    .withExpiresAt(createExpireToken())
                    .sign(algoritmo);
        } catch (JWTCreationException exception) {
            throw exception;
        }
    }

    public String getSubject(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("corrida_api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception){
            throw exception;
        }
    }

    public Instant createExpireToken() {
        return LocalDateTime.now().plusHours(12).toInstant(ZoneOffset.of("-03:00"));
    }
}
