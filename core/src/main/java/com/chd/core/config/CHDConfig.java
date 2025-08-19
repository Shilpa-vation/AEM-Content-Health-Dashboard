package com.chd.core.config;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.AttributeDefinition;

@ObjectClassDefinition(name = "Content Health Dashboard Config")
public @interface CHDConfig {

    @AttributeDefinition(name = "Site Page Path", type = AttributeType.STRING)
    String contentRootPath() default "/content/chd";

   @AttributeDefinition(name = "Asset Page Path", type = AttributeType.STRING)
    String assetRootPath() default "/content/dam/chd";
;

}