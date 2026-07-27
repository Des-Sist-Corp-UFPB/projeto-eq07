package br.ufpb.dsc.corrida.audit;

public record AuditContextSnapshot(
        String userId,
        String requestId,
        String clientIp,
        String userAgent,
        String httpMethod,
        String resource,
        Integer statusCode
) {}
