package br.ufpb.dsc.corrida.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Ação executada. Exemplo: "CORRIDA_CRIADA", "USUARIO_ATUALIZADO".
     */
    String action();

    /**
     * Recurso afetado. Exemplo: "Corrida", "Usuario".
     */
    String resource() default "";
}
