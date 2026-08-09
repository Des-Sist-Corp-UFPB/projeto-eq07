package br.ufpb.dsc.corrida.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import br.ufpb.dsc.corrida.organizer.OrganizerService;
import br.ufpb.dsc.corrida.userConections.UserConnectionService;

@ControllerAdvice
public class GlobalModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalModelAdvice.class);

    @Autowired
    private UserConnectionService userConnectionService;

    @Autowired
    private OrganizerService organizerService;

    @Autowired
    private UserInfoService userInfoService;

    @ModelAttribute("pendingRequestsCount")
    public Long addPendingRequestsCount(@AuthenticationPrincipal User loggedInUser) {
        if (loggedInUser != null) {
            return userConnectionService.getPendingRequestsCount(loggedInUser.getId());
        }
        return 0L;
    }

    @ModelAttribute("userOrganizationId")
    public Long addUserOrganizationId(@AuthenticationPrincipal User loggedInUser) {
        if (loggedInUser != null && loggedInUser.getPapel() == Papel.ORGANIZADOR) {
            return organizerService.buscarOrganizadorPorUsuarioId(loggedInUser.getId())
                    .flatMap(organizer -> organizerService.buscarOrganizacaoPorOrganizadorId(organizer.getId()))
                    .map(br.ufpb.dsc.corrida.organizer.Organization::getId)
                    .orElse(null);
        }
        return null;
    }

    @ModelAttribute("userProfilePhotoUrl")
    public String addUserProfilePhotoUrl(@AuthenticationPrincipal User loggedInUser) {
        if (loggedInUser != null) {
            try {
                String url = userInfoService.buscarPorUsuarioId(loggedInUser.getId()).fotoPerfil();
                log.debug("USER PROFILE PHOTO URL FOR {} IS: {}", loggedInUser.getUsername(), url);
                return url;
            } catch (Exception e) {
                log.debug("ERROR GETTING PROFILE PHOTO URL FOR {}: {}", loggedInUser.getUsername(), e.getMessage());
            }
        }
        return null;
    }
}
