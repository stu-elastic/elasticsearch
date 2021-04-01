/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.rest.action.service;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.core.security.action.service.GetServiceAccountTokensAction;
import org.elasticsearch.xpack.core.security.action.service.GetServiceAccountTokensRequest;
import org.elasticsearch.xpack.security.rest.action.SecurityBaseRestHandler;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.GET;

public class RestGetServiceAccountTokensAction extends SecurityBaseRestHandler {

    public RestGetServiceAccountTokensAction(Settings settings, XPackLicenseState licenseState) {
        super(settings, licenseState);
    }

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(GET, "/_security/service/{namespace}/{service}/credential")
        );
    }

    @Override
    public String getName() {
        return "xpack_security_get_service_account_tokens";
    }

    @Override
    protected RestChannelConsumer innerPrepareRequest(RestRequest request, NodeClient client) throws IOException {
        final GetServiceAccountTokensRequest getServiceAccountTokensRequest =
            new GetServiceAccountTokensRequest(request.param("namespace"), request.param("service"));
        return channel -> client.execute(GetServiceAccountTokensAction.INSTANCE,
            getServiceAccountTokensRequest,
            new RestToXContentListener<>(channel));
    }
}
