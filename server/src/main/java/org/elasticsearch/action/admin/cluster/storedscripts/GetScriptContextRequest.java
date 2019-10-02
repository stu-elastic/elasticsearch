package org.elasticsearch.action.admin.cluster.storedscripts;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.master.MasterNodeReadRequest;
import org.elasticsearch.common.io.stream.StreamInput;

import java.io.IOException;

public class GetScriptContextRequest extends MasterNodeReadRequest<GetScriptContextRequest> {
    public GetScriptContextRequest() {
        super();
    }

    public GetScriptContextRequest(StreamInput in) throws IOException { super(in); }

    @Override
    public ActionRequestValidationException validate() { return null; }

    @Override
    public String toString() {
        return "get script context";
    }
}
