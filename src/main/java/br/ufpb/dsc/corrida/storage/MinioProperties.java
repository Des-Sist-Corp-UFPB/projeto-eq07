package br.ufpb.dsc.corrida.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpointInternal;
    private String endpointPublic;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private String region;
    private String console;
    private int presignedUrlExpirySeconds;
}
