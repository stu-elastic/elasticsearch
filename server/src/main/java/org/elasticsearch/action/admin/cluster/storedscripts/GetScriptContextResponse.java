/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.admin.cluster.storedscripts;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.StatusToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.ScriptExecuteInfo;
import org.elasticsearch.script.ScriptService;

import java.io.IOException;
import java.util.List;

public class GetScriptContextResponse extends ActionResponse implements StatusToXContentObject {

    private List<ScriptService.ScriptContextInfo> contexts;

    GetScriptContextResponse(StreamInput in) throws IOException {
        super(in);
        // TODO(stu): read in list?
    }

    GetScriptContextResponse(List<ScriptService.ScriptContextInfo> contexts) {
        this.contexts = contexts;
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        for (ScriptService.ScriptContextInfo context: this.contexts) {
            out.writeString(context.name);
        }
    }

    @Override public RestStatus status() {
        return contexts != null ? RestStatus.OK : RestStatus.NOT_FOUND;
    }

    @Override public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject().startObject("contexts");
        for (ScriptService.ScriptContextInfo context: this.contexts) {
            builder.startObject(context.name).startObject("method");
            builder.field("name", context.execute.name);
            builder.field("return_type", context.execute.returnType);
            builder.startArray("params");
            for (ScriptExecuteInfo.ParameterInfo param: context.execute.parameters) {
                builder.startObject();
                builder.field("type", param.type).field("name", param.name);
                builder.endObject();
            }
            builder.endArray(); // params
            builder.endObject().endObject(); // method, context.name
        }
        builder.endObject().endObject(); // contexts
        return builder;
    }
}
