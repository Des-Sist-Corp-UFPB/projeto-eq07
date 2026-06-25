
package br.ufpb.dsc.corrida.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3Config {

    private final S3Properties s3Properties;

    public S3Config(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
    }

    @Bean
    public S3Client s3Client() {
        // REDE DE SEGURANÇA: Se o endpoint vier nulo, força o padrão interno do Docker
        String endpoint = s3Properties.getEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            endpoint = "http://minio:9000";
        }

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(s3Properties.getRegion() != null ? s3Properties.getRegion() : "us-east-1"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            s3Properties.getAccessKey() != null ? s3Properties.getAccessKey() : "eq07", 
                            s3Properties.getSecretKey() != null ? s3Properties.getSecretKey() : "minioadmin"
                        )
                ))
                .forcePathStyle(true) 
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        // REDE DE SEGURANÇA: Se o endpoint público vier nulo, força o padrão do servidor
        String publicEndpoint = s3Properties.getPublicEndpoint();
        if (publicEndpoint == null || publicEndpoint.trim().isEmpty()) {
            publicEndpoint = "https://s3.dsc.rodrigor.com";
        }

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        return S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .region(Region.of(s3Properties.getRegion() != null ? s3Properties.getRegion() : "us-east-1"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            s3Properties.getAccessKey() != null ? s3Properties.getAccessKey() : "eq07", 
                            s3Properties.getSecretKey() != null ? s3Properties.getSecretKey() : "minioadmin"
                        )
                ))
                .serviceConfiguration(serviceConfiguration)
                .build();
    }
}