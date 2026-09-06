package com.anjertech.auth.auth_app_backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
//this is to customize our own swagger openApi doc via the url http://localhost:8083/swagger-ui/index.html
@OpenAPIDefinition(
        info = @Info(
                title = "Auth Application build by Anthony Jerry.",
                description = "Generic auth app that can be used with any application.",
                contact = @Contact(
                        name = "Anthony Jery K.",
                        url = "https://www.anjertechnolgies.com/",
                        email = "tonyjerry001@gmail.com"
                ),
                version = "1.0",
                summary = "This app is very useful if you dont want to create auth app from scratch"
        ),
        //this initializes the security
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)
//this initializes an authorization link in our customized open API, protecting all apis
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",  //authorization: Bearer htokenaswga
        bearerFormat = "JWT"
)
public class APIDocConfig {
}
