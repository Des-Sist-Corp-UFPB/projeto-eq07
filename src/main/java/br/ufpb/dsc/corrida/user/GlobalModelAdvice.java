package br.ufpb.dsc.corrida.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import br.ufpb.dsc.corrida.userConections.UserConnectionService;

@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired
    private UserConnectionService userConnectionService;

    @ModelAttribute("pendingRequestsCount")
    public Long addPendingRequestsCount(@AuthenticationPrincipal User loggedInUser) {
        if (loggedInUser != null) {
            return userConnectionService.getPendingRequestsCount(loggedInUser.getId());
        }
        return 0L;
    }
}
