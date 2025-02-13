/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.keycloak.setup;

import io.airbyte.commons.auth.config.AirbyteKeycloakConfiguration;
import io.airbyte.commons.auth.config.OidcConfig;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import org.keycloak.admin.client.resource.RealmResource;

/**
 * This class provides services for managing configuration maps. It includes methods for adding,
 * removing, and updating configuration settings.
 */
@Singleton
public class ConfigurationMapService {

  public static final String HTTPS_PREFIX = "https://";
  public static final String WELL_KNOWN_OPENID_CONFIGURATION_SUFFIX = ".well-known/openid-configuration";
  private final String webappUrl;
  private final AirbyteKeycloakConfiguration keycloakConfiguration;

  public ConfigurationMapService(@Value("${airbyte.webapp-url}") final String webappUrl,
                                 final AirbyteKeycloakConfiguration keycloakConfiguration) {
    this.webappUrl = webappUrl;
    this.keycloakConfiguration = keycloakConfiguration;
  }

  public Map<String, String> importProviderFrom(final RealmResource keycloakRealm,
                                                final OidcConfig oidcConfig,
                                                String keycloakProviderId) {
    Map<String, Object> map = new HashMap<>();
    map.put("providerId", keycloakProviderId);
    map.put("fromUrl", getProviderDiscoveryUrl(oidcConfig));
    return keycloakRealm.identityProviders().importFrom(map);
  }

  public Map<String, String> setupProviderConfig(final OidcConfig oidcConfig, Map<String, String> configMap) {
    Map<String, String> config = new HashMap<>();

    // Copy all keys from configMap to the result map
    config.putAll(configMap);
    // Explicitly set required keys
    config.put("clientId", oidcConfig.clientId());
    config.put("clientSecret", oidcConfig.clientSecret());
    config.put("defaultScope", "openid email profile");
    config.put("redirectUris", getProviderRedirectUrl(oidcConfig));
    config.put("backchannelSupported", "true");
    config.put("backchannel_logout_session_supported", "true");

    return config;
  }

  private String getProviderRedirectUrl(final OidcConfig oidcConfig) {
    final String webappUrlWithTrailingSlash = webappUrl.endsWith("/") ? webappUrl : webappUrl + "/";
    return webappUrlWithTrailingSlash + "auth/realms/" + keycloakConfiguration.getAirbyteRealm() + "/broker/" + oidcConfig.appName() + "/endpoint";
  }

  private String getProviderDiscoveryUrl(final OidcConfig oidcConfig) {
    String domain = oidcConfig.domain();
    if (!domain.startsWith(HTTPS_PREFIX)) {
      domain = HTTPS_PREFIX + domain;
    }
    if (!domain.endsWith(WELL_KNOWN_OPENID_CONFIGURATION_SUFFIX)) {
      domain = domain.endsWith("/") ? domain : domain + "/";
      domain = domain + WELL_KNOWN_OPENID_CONFIGURATION_SUFFIX;
    }
    return domain;
  }

}
