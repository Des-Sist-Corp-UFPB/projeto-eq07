package br.ufpb.dsc.corrida.ors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geo")
public class GeocodingApiController {

    @Autowired
    private OpenRouteServiceClient orsClient;

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchAddress(@RequestParam("text") String text) {
        String jsonResult = orsClient.geocodificarEndereco(text);
        return ResponseEntity.ok(jsonResult);
    }
}
