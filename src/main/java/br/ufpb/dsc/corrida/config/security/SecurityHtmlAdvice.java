package br.ufpb.dsc.corrida.config.security;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SecurityHtmlAdvice {
    @ModelAttribute("_csrf")
    public CsrfToken prependCsrfToken(CsrfToken token) {
        return token;
    }
}
