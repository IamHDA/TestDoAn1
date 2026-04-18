package com.vn.backend.configs;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class PropertiesConfig {
    @Value("${file.path}")
    private String officePath;

    @Value("${url.download.file}")
    private String downloadUrlFile;
}
