/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.as.common.model.registration.Client;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ClientAuthService;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;

@Path(ApiConstants.CLIENTS + ApiConstants.AUTHORIZATIONS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ClientAuthResource extends ConfigBaseResource {

	@Inject
	ClientAuthService clientAuthService;

	@Inject
	ClientService clientService;

	private class AuthMap extends HashMap<Client, Set<Scope>> {
	};

	@Operation(summary = "Gets list of client authorizations", description = "Gets list of client authorizations", operationId = "get-client-authorizations", tags = {
			"OpenID Connect - Clients - Authorizations" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS }))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = HashMap.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/clients-get-all.json"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "InternalServerError") })
	@GET
	@ProtectedApi(scopes = { ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS }, groupScopes = {
			ApiAccessConstants.OPENID_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
	@Path(ApiConstants.USERID_PATH)
	public Response getClientAuthorizations(
			@Parameter(description = "Client Auth user ID") @PathParam(ApiConstants.USERID) @NotNull String userId) {

		logger.error("Client serach param - user:{}", escapeLog(userId));

		Map<Client, Set<Scope>> clientAuths = clientAuthService.getUserClientAuthorizationData(userId);
		logger.error("Client serach param - clientAuths:{}", clientAuths);
		return Response.ok(clientAuths).build();
	}

	@Operation(summary = "Gets all client authorizations", description = "Gets list of client authorizations", operationId = "get-client-authorizations", tags = {
			"OpenID Connect - Clients - Authorizations" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS }))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = HashMap.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/clients-get-all.json"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "InternalServerError") })
	@GET
	@ProtectedApi(scopes = { ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS }, groupScopes = {
			ApiAccessConstants.OPENID_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
	@Path("/test1")
	public Response getAllAuthorizations() {

		logger.error("getAllAuthorizations()");
		Map<String, HashMap<String, String>> nestedClientMap = new HashMap<>();

		for (int i = 1; i <= 10; i++) {
			HashMap<String, String> clientMap = new HashMap<>();
			clientMap.put("" + i, "Client" + i);
			nestedClientMap.put("Nested" + i, clientMap);
		}

		logger.error("Client serach param - nestedClientMap:{}", nestedClientMap);
		return Response.ok(nestedClientMap).build();
	}

	@Operation(summary = "Gets all client authorizations", description = "Gets list of client authorizations", operationId = "get-client-authorizations", tags = {
			"OpenID Connect - Clients - Authorizations" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS }))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = HashMap.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/clients-get-all.json"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "InternalServerError") })
	@GET
	@ProtectedApi(scopes = { ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS }, groupScopes = {
			ApiAccessConstants.OPENID_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
	@Path("/test2")
	public Response getAllAuthorizations2() {

		logger.error("getAllAuthorizations()");
		Map<String, Client> nestedClientMap = new HashMap<>();
		List<Client> clients = clientService.getAllClients(10);
		if (clients != null && !clients.isEmpty()) {
			for (int i = 0; i < clients.size(); i++) {
				nestedClientMap.put("Client" + i, clients.get(i));
			}
		}

		logger.error("Client serach param - nestedClientMap:{}", nestedClientMap);
		return Response.ok(nestedClientMap).build();
	}

}
