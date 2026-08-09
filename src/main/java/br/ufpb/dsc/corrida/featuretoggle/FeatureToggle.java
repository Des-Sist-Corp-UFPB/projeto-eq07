package br.ufpb.dsc.corrida.featuretoggle;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um método ou classe para ser controlado por um Feature Flag persistido no banco de dados.
 *
 * <h3>Comportamento</h3>
 * <ul>
 *   <li>Se o flag estiver <b>habilitado</b> ({@code enabled = true} no BD), o método é executado normalmente.</li>
 *   <li>Se o flag estiver <b>desabilitado</b> ({@code enabled = false}) e nenhum {@link #fallbackMethod()}
 *       for especificado, uma {@link FeatureDisabledException} é lançada (HTTP 503).</li>
 *   <li>Se o flag estiver <b>desabilitado</b> e um {@link #fallbackMethod()} for especificado, o método
 *       alternativo é invocado no lugar do método original.</li>
 * </ul>
 *
 * <h3>Precedência de Anotação</h3>
 * A anotação aplicada ao nível de <b>MÉTODO sempre sobrepõe</b> a anotação aplicada ao nível de
 * <b>CLASSE (TYPE)</b>. Isso permite que métodos individuais usem flags diferentes do padrão da classe.
 *
 * <h3>Contrato do {@code fallbackMethod}</h3>
 * <ul>
 *   <li>O método de fallback <b>deve estar definido na mesma classe</b> que o método original.</li>
 *   <li>A assinatura do {@code fallbackMethod} <b>deve ser idêntica</b> ao método original:
 *       mesmos tipos de parâmetros (na mesma ordem) e mesmo tipo de retorno.</li>
 *   <li>Se o {@code fallbackMethod} não existir na classe, uma {@link IllegalStateException}
 *       é lançada imediatamente (falha explícita, nunca silenciosa).</li>
 * </ul>
 *
 * <h3>Limitação de Auto-invocação</h3>
 * Chamadas internas ({@code this.metodo()}) dentro do mesmo bean <b>não passam pelo proxy Spring AOP</b>
 * e portanto <b>não serão interceptadas</b>. Invoque sempre via referência ao bean gerenciado.
 *
 * <h3>Exemplo de uso</h3>
 * <pre>{@code
 * // No nível de método (recomendado)
 * @FeatureToggle(value = "PAYMENT_V2", fallbackMethod = "processPaymentLegacy")
 * public PaymentResult processPayment(PaymentRequest req) { ... }
 *
 * public PaymentResult processPaymentLegacy(PaymentRequest req) { ... }
 *
 * // No nível de classe
 * @FeatureToggle("PAYMENT_V2")
 * public class PaymentService { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeatureToggle {

    /**
     * Chave do Feature Flag no banco de dados (ex: "PAYMENT_V2", "AUDIT_NEW_PIPELINE").
     * <p>Este valor é obrigatório e deve corresponder exatamente ao {@code key_name} cadastrado na
     * tabela {@code feature_flags}.
     *
     * @return a chave do feature flag
     */
    String value();

    /**
     * Nome do método alternativo a ser invocado quando o feature flag estiver desabilitado.
     *
     * <p>O método deve estar declarado na mesma classe e ter <b>exatamente a mesma assinatura</b>
     * (tipos de parâmetros e tipo de retorno) que o método original.
     *
     * <p>Deixar em branco (padrão) faz com que uma {@link FeatureDisabledException} seja lançada
     * quando o flag estiver desabilitado.
     *
     * @return nome do método de fallback, ou {@code ""} se nenhum for especificado
     */
    String fallbackMethod() default "";
}
