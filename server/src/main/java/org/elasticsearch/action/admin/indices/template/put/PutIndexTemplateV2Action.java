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

package org.elasticsearch.action.admin.indices.template.put;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.IndexTemplateV2;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.regex.Regex;

import java.io.IOException;
import java.util.Objects;

import static org.elasticsearch.action.ValidateActions.addValidationError;

public class PutIndexTemplateV2Action extends ActionType<AcknowledgedResponse> {

    public static final PutIndexTemplateV2Action INSTANCE = new PutIndexTemplateV2Action();
    public static final String NAME = "indices:admin/index_template/put";

    private PutIndexTemplateV2Action() {
        super(NAME, AcknowledgedResponse::new);
    }

    /**
     * A request for putting a single index template into the cluster state
     */
    public static class Request extends MasterNodeRequest<Request> implements IndicesRequest {
        private final String name;
        @Nullable
        private String cause;
        private boolean create;
        private IndexTemplateV2 indexTemplate;

        public Request(StreamInput in) throws IOException {
            super(in);
            this.name = in.readString();
            this.cause = in.readOptionalString();
            this.create = in.readBoolean();
            this.indexTemplate = new IndexTemplateV2(in);
        }

        /**
         * Constructs a new put index template request with the provided name.
         */
        public Request(String name) {
            this.name = name;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(name);
            out.writeOptionalString(cause);
            out.writeBoolean(create);
            this.indexTemplate.writeTo(out);
        }

        @Override
        public ActionRequestValidationException validate() {
            ActionRequestValidationException validationException = null;
            if (name == null || Strings.hasText(name) == false) {
                validationException = addValidationError("name is missing", validationException);
            }
            if (indexTemplate == null) {
                validationException = addValidationError("an index template is required", validationException);
            } else {
                if (indexTemplate.indexPatterns().stream().anyMatch(Regex::isMatchAllPattern)) {
                    if (IndexMetadata.INDEX_HIDDEN_SETTING.exists(indexTemplate.template().settings())) {
                        validationException = addValidationError("global V2 templates may not specify the setting "
                                + IndexMetadata.INDEX_HIDDEN_SETTING.getKey(),
                            validationException
                        );
                    }
                }
            }

            return validationException;
        }

        /**
         * The name of the index template.
         */
        public String name() {
            return this.name;
        }

        /**
         * Set to {@code true} to force only creation, not an update of an index template. If it already
         * exists, it will fail with an {@link IllegalArgumentException}.
         */
        public Request create(boolean create) {
            this.create = create;
            return this;
        }

        public boolean create() {
            return create;
        }

        /**
         * The cause for this index template creation.
         */
        public Request cause(@Nullable String cause) {
            this.cause = cause;
            return this;
        }

        @Nullable
        public String cause() {
            return this.cause;
        }

        /**
         * The index template that will be inserted into the cluster state
         */
        public Request indexTemplate(IndexTemplateV2 template) {
            this.indexTemplate = template;
            return this;
        }

        public IndexTemplateV2 indexTemplate() {
            return this.indexTemplate;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("PutTemplateV2Request[");
            sb.append("name=").append(name);
            sb.append(", cause=").append(cause);
            sb.append(", create=").append(create);
            sb.append(", index_template=").append(indexTemplate);
            sb.append("]");
            return sb.toString();
        }

        @Override
        public String[] indices() {
            return indexTemplate.indexPatterns().toArray(Strings.EMPTY_ARRAY);
        }

        @Override
        public IndicesOptions indicesOptions() {
            return IndicesOptions.strictExpand();
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, cause, create, indexTemplate);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Request other = (Request) obj;
            return Objects.equals(this.name, other.name) &&
                Objects.equals(this.cause, other.cause) &&
                Objects.equals(this.indexTemplate, other.indexTemplate) &&
                this.create == other.create;
        }
    }

}
