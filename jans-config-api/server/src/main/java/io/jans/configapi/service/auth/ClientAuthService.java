package io.jans.configapi.service.auth;

import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import io.jans.as.persistence.model.ClientAuthorization;
import io.jans.orm.PersistenceEntryManager;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.persistence.model.Scope;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ClientAuthService {

	@Inject
	private Logger logger;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	StaticConfiguration staticConfiguration;

	@Inject
	private ClientService clientService;

	@Inject
	ScopeService scopeService;

	@Inject
	private OrganizationService organizationService;

	public Map<Client, Set<Scope>> getUserAuthorizations(String userId) {

		logger.debug(" Authorizations details to be fetched for userId:{} ", userId);

		ClientAuthorization clientAuthorization = persistenceEntryManager.find(ClientAuthorization.class,
				getClientAuthorizationDn(userId));
		logger.debug("{} client-authorization entries found", clientAuthorization);

		ClientAuthorization clientAuth = new ClientAuthorization();
		clientAuth.setId(userId);
		List<ClientAuthorization> authorizations = persistenceEntryManager.findEntries(clientAuth);
		logger.debug("{} client-authorization entries found", authorizations);

		if (authorizations == null || authorizations.isEmpty()) {
			return Collections.emptyMap();
		}

		// Obtain client ids from all this user's client authorizations
		Set<String> clientIds = authorizations.stream().map(ClientAuthorization::getClientId)
				.collect(Collectors.toSet());

		// Create a filter based on client Ids, alternatively one can make n queries to
		// obtain client references one by one
		Filter[] filters = clientIds.stream().map(id -> Filter.createEqualityFilter("inum", id))
				.collect(Collectors.toList()).toArray(new Filter[] {});
		List<Client> clients = persistenceEntryManager.findEntries(clientService.getDnForClient(null), Client.class,
				Filter.createORFilter(filters));

		Set<String> scopeIds = authorizations.stream().map(ClientAuthorization::getScopes).flatMap(Arrays::stream)
				.collect(Collectors.toSet());

		// Do the analog for scopes
		filters = scopeIds.stream().map(id -> Filter.createEqualityFilter("jansId", id)).collect(Collectors.toList())
				.toArray(new Filter[] {});
		List<Scope> scopes = persistenceEntryManager.findEntries(scopeService.getDnForScope(null), Scope.class,
				Filter.createORFilter(filters));

		logger.debug("Found {} client authorizations for user {}", clients.size(), userId);
		Map<Client, Set<Scope>> perms = new HashMap<>();

		for (Client client : clients) {
			Set<Scope> clientScopes = new HashSet<>();
			logger.debug("client:{}", client);
			for (ClientAuthorization auth : authorizations) {
				logger.debug("auth:{}", auth);
				if (auth.getClientId().equals(client.getClientId())) {
					for (String scopeName : auth.getScopes()) {
						scopes.stream().filter(sc -> sc.getId().equals(scopeName)).findAny()
								.ifPresent(clientScopes::add);
					}
				}
			}
			perms.put(client, clientScopes);
		}
		logger.debug("perms {}", perms);
		return perms;
	}

	public List<ClientAuthorization> getClientAuthorizations(String clientId) {
		logger.debug(" Authorizations details to be fetched bassed on clientId:{} ", clientId);
		Filter clientIdFilter = Filter.createEqualityFilter("jansClntId", clientId);
		List<ClientAuthorization> clientAuthorizations = persistenceEntryManager
				.findEntries(getClientAuthorizationDn(null), ClientAuthorization.class, clientIdFilter);
		logger.debug(" Authorizations details to be fetched for clientAuthorizations:{} ", clientAuthorizations);
		return clientAuthorizations;
	}

	public Map<Client, Set<Scope>> getUserClientAuthorizationData(String userId) {
		logger.debug(" Authorizations details to be fetched for userId:{} ", userId);

		Filter clientIdFilter = Filter.createEqualityFilter("jansUsrId", userId);
		List<ClientAuthorization> clientAuthorizations = persistenceEntryManager
				.findEntries(getClientAuthorizationDn(null), ClientAuthorization.class, clientIdFilter);
		logger.debug(" Authorizations details to be fetched for clientAuthorizations:{} ", clientAuthorizations);

		if (clientAuthorizations == null || clientAuthorizations.isEmpty())
			return Collections.emptyMap();

		// Obtain client ids from all this user's client authorizations
		Set<String> clientIds = clientAuthorizations.stream().map(ClientAuthorization::getClientId)
				.collect(Collectors.toSet());
		logger.debug(" Authorizations clientIds:{} ", clientIds);
		Filter[] filters = clientIds.stream().map(id -> Filter.createEqualityFilter("inum", id))
				.collect(Collectors.toList()).toArray(new Filter[] {});
		List<Client> clients = persistenceEntryManager.findEntries(clientService.getDnForClient(null), Client.class,
				Filter.createORFilter(filters));
		logger.debug(" Authorizations clients:{} ", clients);

		List<Scope> scopes = getScopeList(clientAuthorizations);
		logger.debug(" Authorizations scopes:{} ", scopes);

		Map<Client, Set<Scope>> perms = new HashMap<>();

		for (Client client : clients) {
			Set<Scope> clientScopes = new HashSet<>();

			for (ClientAuthorization auth : clientAuthorizations) {
				if (auth.getClientId().equals(client.getClientId())) {
					for (String scopeName : auth.getScopes()) {
						scopes.stream().filter(sc -> sc.getId().equals(scopeName)).findAny()
								.ifPresent(clientScopes::add);
					}
				}
			}
			perms.put(client, clientScopes);
		}

		return perms;
	}

	public String getClientAuthorizationDn(String id) {
		String baseDn = staticConfiguration.getBaseDn().getAuthorizations();
		if (StringUtils.isEmpty(id)) {
			return baseDn;
		}
		return String.format("jansId=%s,%s", id, baseDn);
	}

	public String getDnForClient(String inum) {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=clients,%s", orgDn);
		}
		return String.format("inum=%s,ou=clients,%s", inum, orgDn);
	}

	private List<Scope> getScopeList(List<ClientAuthorization> clientAuthorizations) {
		logger.debug("Client authorizations for clientAuthorizations {}", clientAuthorizations);
		Set<String> scopeIds = new HashSet<>();
		for (ClientAuthorization auth : clientAuthorizations) {
			logger.debug("Client authorizations for auth.getClientId():{}, auth.getScopes():{}", auth.getClientId(),
					auth.getScopes());
			if (auth.getScopes() != null && auth.getScopes().length > 0) {
				Arrays.asList(auth.getScopes()).stream().filter(scopeIds::add);
			}
		}
		logger.debug("scopeIds:{}", scopeIds);

		return scopeService.searchScopesById(scopeIds);

	}

}
