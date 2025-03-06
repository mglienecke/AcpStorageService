package uk.ac.ed.acpstorageservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ed.acpstorageservice.data.RuntimeEnvironment;

@Configuration
public class AppConfig {
    @Bean
    public RuntimeEnvironment CurrentRuntimeEnvironment() {
        return RuntimeEnvironment.getEnvironment();
    }
}
