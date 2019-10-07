package org.elasticsearch.action.admin.cluster.storedscripts;

import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.support.master.MasterNodeReadOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

public class GetScriptContextRequestBuilder extends MasterNodeReadOperationRequestBuilder<GetScriptContextRequest,
    GetScriptContextResponse, GetScriptContextRequestBuilder> {

    public GetScriptContextRequestBuilder(ElasticsearchClient client, ActionType<GetScriptContextResponse> action) {
        super(client, action, new GetScriptContextRequest());
    }
}
