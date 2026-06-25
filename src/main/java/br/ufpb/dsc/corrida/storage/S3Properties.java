package br.ufpb.dsc.corrida.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {
    private String endpoint;
    private String publicEndpoint;
    private String region;
    private String bucket;
    private String accessKey;
    private String secretKey;
}
