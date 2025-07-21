package com.chd.core.service;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import com.chd.core.config.CHDConfig;

@Component(service = CHDService.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CHDService {

    private String contentRootPath;
    private String assetRootPath;

    @Activate
    @Modified
    protected void activate(CHDConfig config) {
        this.contentRootPath = config.contentRootPath();
        this.assetRootPath = config.assetRootPath();
    }

}