package br.ufpb.dsc.corrida.config.cors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") 
                .allowedOrigins("*") 
                .allowedMethods("GET", "PUT", "DELETE", "POST", "PATCH") 
                .allowedHeaders("*");
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Diz ao Spring que qualquer requisição (/**) deve tentar 
        // buscar o arquivo correspondente dentro de src/main/resources/public/
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/public/");
    }
}