package it.gov.pagopa.payment.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class SwaggerConfig.
 */
@Configuration
public class SwaggerConfig {

	/** The title. */
	@Value("${swagger.title:${spring.application.name}}")
	private String title;

	/** The description. */
	@Value("${swagger.description:Api and Models}")
	private String description;

	/** The version. */
	@Value("${swagger.version:${spring.application.version}}")
	private String version;

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI().components(new Components()).info(new Info()
				.title(title)
				.description(description)
				.version(version));
	}

	@Bean
	public GroupedOpenApi apiIo() {
		return GroupedOpenApi.builder()
				.displayName(title + "-" + "IO")
				.group("IO")
				.pathsToExclude("/**/merchant/**")
				.build();
	}

	@Bean
	public GroupedOpenApi apiAcquirer() {
		return GroupedOpenApi.builder()
				.displayName(title + "-" + "MERCHANT")
				.group("MERCHANT")
				.pathsToMatch("/**/merchant/**")
				.build();
	}

}