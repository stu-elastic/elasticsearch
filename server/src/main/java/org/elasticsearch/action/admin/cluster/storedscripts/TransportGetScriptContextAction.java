package org.elasticsearch.action.admin.cluster.storedscripts;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeReadAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;

public class TransportGetScriptContextAction extends TransportMasterNodeReadAction<GetScriptContextRequest,
    GetScriptContextResponse> {
    private final ScriptService scriptService;

    @Inject
    public TransportGetScriptContextAction(TransportService transportService, ClusterService clusterService,
        ThreadPool threadPool, ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver, ScriptService scriptService) {
        super(GetScriptContextAction.NAME, transportService, clusterService, threadPool, actionFilters,
            GetScriptContextRequest::new, indexNameExpressionResolver);
        this.scriptService = scriptService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected GetScriptContextResponse read(StreamInput in) throws IOException {
        return new GetScriptContextResponse(in);
    }

    @Override
    protected void masterOperation(Task task, GetScriptContextRequest request, ClusterState state,
        ActionListener<GetScriptContextResponse> listener) throws Exception {
        listener.onResponse(new GetScriptContextResponse(scriptService.getContextInfos()));
    }

    @Override
    protected ClusterBlockException checkBlock(GetScriptContextRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
