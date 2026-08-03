package br.ufpb.dsc.corrida.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entidade que armazena dados físicos e médicos de um corredor.
 * Possui relação 1:1 com a entidade {@link User}.
 */
@Entity
@Table(name = "user_info")
@Getter
@Setter
@NoArgsConstructor
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Relação 1:1 com a tabela de usuários. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private User usuario;

    /** Peso do corredor em quilogramas. Deve ser maior que 0. */
    @Column(nullable = false)
    private Float peso;

    /** Altura do corredor em centímetros. Deve ser maior que 0. */
    @Column(nullable = false)
    private Float altura;

    /** Gênero do corredor. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genero genero;

    /** Total de quilômetros percorridos pelo corredor. Valor padrão: 0. */
    @Column(name = "total_km_run", nullable = false)
    private Float totalKmRun = 0.0f;

    /** Data de nascimento do corredor. */
    @Column(name = "data_nasc", nullable = false)
    private LocalDate dataNasc;

    /** URL da foto de perfil do corredor. Opcional. */
    @Column(name = "foto_perfil")
    private String fotoPerfil;

    /** Nível de condicionamento físico do corredor. Opcional. */
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_condicionamento")
    private NivelCondicionamento nivelCondicionamento;

    /** Observações médicas relevantes, lesões ou restrições. Opcional. */
    @Column(name = "notas_medicas", columnDefinition = "TEXT")
    private String notasMedicas;

    /** Consentimento para processamento de dados sensíveis de saúde via LLM. */
    @Column(name = "consentimento_saude", nullable = false)
    private Boolean consentimentoSaude = false;

    /**
     * CPF do atleta — armazenado somente com dígitos (11 chars, sem máscara).
     * Nullable no banco; obrigatoriedade aplicada na camada de aplicação
     * apenas para inscrições em corridas pagas (valorInscricao > 0).
     */
    @Column(name = "cpf", length = 11, unique = true)
    private String cpf;

    /** Timestamp de criação do registro (gerado automaticamente). */
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    /** Timestamp da última atualização do registro (atualizado automaticamente). */
    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    /**
     * Retorna o CPF formatado para exibição na UI (000.000.000-00).
     * Retorna null se o CPF não estiver cadastrado.
     */
    public String getCpfFormatado() {
        if (cpf == null || cpf.length() != 11) return null;
        return cpf.substring(0, 3) + "." +
               cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" +
               cpf.substring(9, 11);
    }
}
