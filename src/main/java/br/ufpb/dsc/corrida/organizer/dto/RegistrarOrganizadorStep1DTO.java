package br.ufpb.dsc.corrida.organizer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrarOrganizadorStep1DTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Username é obrigatório")
        String username,

        @NotBlank(message = "Login é obrigatório")
        @Email(message = "Login deve ser um e-mail válido")
        String login,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres")
        String senha,

        @NotBlank(message = "CREF é obrigatório")
        @Pattern(regexp = "^[0-9]{4,6}-[GPgp]/[A-Za-z]{2}$", message = "CREF inválido. Formato esperado: XXXXX-G/SP")
        String cref,

        @NotBlank(message = "CPF é obrigatório")
        @Pattern(regexp = "(^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$)|(^\\d{11}$)", message = "CPF inválido. Formato esperado: 123.456.789-00 ou 11 dígitos numéricos")
        String cpf,

        @NotBlank(message = "E-mail de contato é obrigatório")
        @Email(message = "E-mail de contato inválido")
        String email,

        @NotBlank(message = "WhatsApp é obrigatório")
        String whatsapp,

        @NotBlank(message = "UF do Conselho é obrigatório")
        @Pattern(regexp = "^[A-Z]{2}$", message = "UF deve ser composta por 2 letras maiúsculas")
        String ufConselho
) {}
