package uk.ac.ed.acpstorageservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ed.acpstorageservice.controller.provider.Amazon;
import uk.ac.ed.acpstorageservice.controller.provider.Azure;
import uk.ac.ed.acpstorageservice.controller.provider.StorageProvider;
import uk.ac.ed.acpstorageservice.data.RuntimeEnvironment;

@Configuration
public class AppConfig {
    @Bean
    public RuntimeEnvironment CurrentRuntimeEnvironment() {
        return RuntimeEnvironment.getruntimeEnvironment();
    }

    @Bean
    public StorageProvider StorageProvider(RuntimeEnvironment env) {
        return env.isUseAws() ? new Amazon(env) : new Azure(env);
    }
}
