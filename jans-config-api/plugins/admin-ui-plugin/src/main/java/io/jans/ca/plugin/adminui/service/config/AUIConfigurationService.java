package io.jans.ca.plugin.adminui.service.config;

import com.google.api.client.util.Strings;
import com.google.common.collect.Maps;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.LicenseConfig;
import io.jans.as.model.config.adminui.OIDCClientSettings;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.ca.plugin.adminui.model.auth.DCRResponse;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.config.LicenseConfiguration;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.rest.license.LicenseResource;
import io.jans.ca.plugin.adminui.service.BaseService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.EncryptionService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Random;

@Singleton
public class AUIConfigurationService extends BaseService {

    private Map<String, AUIConfiguration> appConfigurationMap;

    @Inject
    Logger logger;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    EncryptionService encryptionService;

    @Inject
    ConfigurationService configurationService;

    public AUIConfiguration getAUIConfiguration() throws Exception {
        return getAUIConfiguration(null);
    }

    /**
     * It reads the configuration from the LDAP server and stores it in a map
     *
     * @param appType The application type. This is either "adminUI" or "ads".
     * @throws Exception
     * @return The AUIConfiguration object
     */
    public AUIConfiguration getAUIConfiguration(String appType) throws Exception {
        logger.info("Inside method to read the configuration from the LDAP server and stores it in a map.");
        try {
            if (Strings.isNullOrEmpty(appType)) {
                appType = AppConstants.APPLICATION_KEY_ADMIN_UI;
            }

            if (appConfigurationMap == null) {
                appConfigurationMap = Maps.newHashMap();
            }
            AUIConfiguration auiConfiguration = null;

            if (appConfigurationMap.get(appType) == null) {
                AdminConf appConf = null;
                if (appType.equals(AppConstants.APPLICATION_KEY_ADMIN_UI)) {
                    appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                } else if (appType.equals(AppConstants.APPLICATION_KEY_ADS)) {
                    appConf = entryManager.find(AdminConf.class, AppConstants.ADS_CONFIG_DN);
                }
                auiConfiguration = addPropertiesToAUIConfiguration(appType, appConf);
                if (!appType.equals(AppConstants.APPLICATION_KEY_ADS)) {
                    auiConfiguration.setLicenseConfiguration(addPropertiesToLicenseConfiguration(appConf));
                    appConfigurationMap.put(appType, auiConfiguration);
                }
            }
            return appConfigurationMap.get(appType);
        } catch (Exception e) {
            logger.error(ErrorResponse.ERROR_READING_CONFIG.getDescription());
            throw e;
        }
    }

    public void setAuiConfiguration(AUIConfiguration auiConfiguration) {
        if (!Strings.isNullOrEmpty(auiConfiguration.getAppType())) {
            this.appConfigurationMap.put(auiConfiguration.getAppType(), auiConfiguration);
        }
    }

    private AUIConfiguration addPropertiesToAUIConfiguration(String appType, AdminConf appConf) {
        AUIConfiguration auiConfig = new AUIConfiguration();
        AppConfiguration appConfiguration = configurationService.find();
        auiConfig.setAppType(appType);
        auiConfig.setAuiWebServerHost(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getOpHost());
        auiConfig.setAuiWebServerClientId(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getClientId());
        auiConfig.setAuiWebServerClientSecret(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getClientSecret());
        auiConfig.setAuiWebServerScope(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getScopes(), "+"));
        auiConfig.setAuiWebServerRedirectUrl(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getRedirectUri());
        auiConfig.setAuiWebServerFrontChannelLogoutUrl(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getFrontchannelLogoutUri());
        auiConfig.setAuiWebServerPostLogoutRedirectUri(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getPostLogoutUri());
        auiConfig.setAuiWebServerAuthzBaseUrl(appConfiguration.getAuthorizationEndpoint());
        auiConfig.setAuiWebServerTokenEndpoint(appConfiguration.getTokenEndpoint());
        auiConfig.setAuiWebServerIntrospectionEndpoint(appConfiguration.getIntrospectionEndpoint());
        auiConfig.setAuiWebServerUserInfoEndpoint(appConfiguration.getUserInfoEndpoint());
        auiConfig.setAuiWebServerEndSessionEndpoint(appConfiguration.getEndSessionEndpoint());
        auiConfig.setAuiWebServerAcrValues(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getAcrValues(), "+"));

        auiConfig.setAuiBackendApiServerClientId(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getClientId());
        auiConfig.setAuiBackendApiServerClientSecret(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getClientSecret());
        auiConfig.setAuiBackendApiServerScope(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getScopes(), "+"));
        auiConfig.setAuiBackendApiServerTokenEndpoint(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getTokenEndpoint());
        auiConfig.setAuiBackendApiServerIntrospectionEndpoint(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getIntrospectionEndpoint());

        auiConfig.setSessionTimeoutInMins(appConf.getMainSettings().getUiConfig().getSessionTimeoutInMins());
        auiConfig.setAdditionalParameters(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getAdditionalParameters());
        return auiConfig;
    }

    private LicenseConfiguration addPropertiesToLicenseConfiguration(AdminConf appConf) {
        logger.info("addPropertiesToLicenseConfiguration :: Inside addPropertiesToLicenseConfiguration.");
        LicenseConfiguration licenseConfiguration = new LicenseConfiguration();
        try {
            LicenseConfig licenseConfig = appConf.getMainSettings().getLicenseConfig();
            logger.info("addPropertiesToLicenseConfiguration :: Inside addPropertiesToLicenseConfiguration. : {}", licenseConfig.toString());
            if (licenseConfig != null) {
                logger.info("addPropertiesToLicenseConfiguration :: licenseConfig is null. ");
                validateLicenseClientOnAuthServer(licenseConfig);
                licenseConfiguration.setHardwareId(licenseConfig.getLicenseHardwareKey());
                licenseConfiguration.setLicenseKey(licenseConfig.getLicenseKey());
                licenseConfiguration.setScanApiHostname(licenseConfig.getScanLicenseApiHostname());
                licenseConfiguration.setScanAuthServerHostname(licenseConfig.getOidcClient().getOpHost());
                licenseConfiguration.setScanApiClientId(licenseConfig.getOidcClient().getClientId());
                licenseConfiguration.setScanApiClientSecret(licenseConfig.getOidcClient().getClientSecret());
            }
            logger.info("addPropertiesToLicenseConfiguration :: licenseConfig is obtained successfully. ");
            return licenseConfiguration;
        } catch (Exception e) {
            logger.error(ErrorResponse.ERROR_IN_LICENSE_CONFIGURATION_VALIDATION.getDescription());
        }
        return null;
    }

    private void validateLicenseClientOnAuthServer(LicenseConfig licenseConfig) throws ApplicationException {
        try {
            Random rand = new Random();
            int rand_int1 = rand.nextInt(10000);
            logger.info("validateLicenseClientOnAuthServer :: Inside method to request license credentials from SCAN api. {}", rand_int1);
            logger.info("validateLicenseClientOnAuthServer :: Before token request {}", rand_int1);
            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfig.getOidcClient().getOpHost(), licenseConfig.getOidcClient().getClientId(), licenseConfig.getOidcClient().getClientSecret());
            logger.info("validateLicenseClientOnAuthServer :: After token request {}", rand_int1);
            if (tokenResponse == null) {
                //try to re-generate clients using old SSA
                logger.info("validateLicenseClientOnAuthServer :: tokenResponse is null {}", rand_int1);
                logger.info("validateLicenseClientOnAuthServer ::Before executeDCR {}", rand_int1);
                DCRResponse dcrResponse = executeDCR(licenseConfig.getSsa());
                logger.info("validateLicenseClientOnAuthServer ::After executeDCR {}", rand_int1);
                if (dcrResponse == null) {
                    logger.info("validateLicenseClientOnAuthServer :: DCR response is null {}", rand_int1);
                    throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_DCR.getDescription());
                }
                logger.info("validateLicenseClientOnAuthServer :: Before generateToken {}", rand_int1);
                tokenResponse = generateToken(licenseConfig.getOidcClient().getOpHost(), licenseConfig.getOidcClient().getClientId(), licenseConfig.getOidcClient().getClientSecret());
                logger.info("validateLicenseClientOnAuthServer :: After generateToken {}", rand_int1);
                if (tokenResponse == null) {
                    logger.info("validateLicenseClientOnAuthServer :: tokenResponse is null {}", rand_int1);
                    throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                }
                logger.info("validateLicenseClientOnAuthServer :: tokenResponse is is successful {}", rand_int1);
                AdminConf appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                LicenseConfig lc = appConf.getMainSettings().getLicenseConfig();
                lc.setScanLicenseApiHostname(dcrResponse.getScanHostname());
                OIDCClientSettings oidcClient = new OIDCClientSettings(dcrResponse.getOpHost(), dcrResponse.getClientId(), dcrResponse.getClientSecret());
                lc.setOidcClient(oidcClient);
                appConf.getMainSettings().setLicenseConfig(lc);
                entryManager.merge(appConf);
            }
        } catch (Exception e) {
            logger.error(ErrorResponse.ERROR_IN_LICENSE_CONFIGURATION_VALIDATION.getDescription());
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_LICENSE_CONFIGURATION_VALIDATION.getDescription());
        }
    }

    private io.jans.as.client.TokenResponse generateToken(String opHost, String clientId, String clientSecret) {
        try {
            logger.info("AUIConfigurationService-generateToken :: Inside generateToken method");

            logger.info("AUIConfigurationService-generateToken :: opHost : {}", opHost);
            logger.info("AUIConfigurationService-generateToken :: clientId : {}", clientId);
            logger.info("AUIConfigurationService-generateToken :: clientSecret : {}", clientSecret);
            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setScope(LicenseResource.SCOPE_LICENSE_READ);
            logger.info("AUIConfigurationService-generateToken :: scope : {}", LicenseResource.SCOPE_LICENSE_READ);
            logger.info("Trying to get access token from auth server: {}", opHost);
            String scanLicenseApiHostname = (new StringBuffer()).append(StringUtils.removeEnd(opHost, "/"))
                    .append("/jans-auth/restv1/token").toString();
            logger.info("AUIConfigurationService-generateToken :: scanLicenseApiHostname : {}", scanLicenseApiHostname);
            io.jans.as.client.TokenResponse tokenResponse = null;
            logger.info("AUIConfigurationService-generateToken :: Before calling  getToken");
            tokenResponse = getToken(tokenRequest, scanLicenseApiHostname);
            logger.info("AUIConfigurationService-generateToken :: After calling  getToken");
            return tokenResponse;
        } catch (Exception e) {
            logger.error("AUIConfigurationService-generateToken :: Error in generating token");
            logger.error(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            return null;
        }
    }


}