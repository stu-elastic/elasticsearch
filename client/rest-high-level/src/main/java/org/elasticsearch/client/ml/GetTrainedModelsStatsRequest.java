/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.client.ml;

import org.elasticsearch.client.Validatable;
import org.elasticsearch.client.ValidationException;
import org.elasticsearch.client.core.PageParams;
import org.elasticsearch.core.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GetTrainedModelsStatsRequest implements Validatable {

    public static final String ALLOW_NO_MATCH = "allow_no_match";

    private final List<String> ids;
    private Boolean allowNoMatch;
    private PageParams pageParams;

    /**
     * Helper method to create a request that will get ALL TrainedModelStats
     * @return new {@link GetTrainedModelsStatsRequest} object for the id "_all"
     */
    public static GetTrainedModelsStatsRequest getAllTrainedModelStatsRequest() {
        return new GetTrainedModelsStatsRequest("_all");
    }

    public GetTrainedModelsStatsRequest(String... ids) {
        this.ids = Arrays.asList(ids);
    }

    public List<String> getIds() {
        return ids;
    }

    public Boolean getAllowNoMatch() {
        return allowNoMatch;
    }

    /**
     * Whether to ignore if a wildcard expression matches no trained models.
     *
     * @param allowNoMatch If this is {@code false}, then an error is returned when a wildcard (or {@code _all})
     *                    does not match any trained models
     */
    public GetTrainedModelsStatsRequest setAllowNoMatch(boolean allowNoMatch) {
        this.allowNoMatch = allowNoMatch;
        return this;
    }

    public PageParams getPageParams() {
        return pageParams;
    }

    public GetTrainedModelsStatsRequest setPageParams(@Nullable PageParams pageParams) {
        this.pageParams = pageParams;
        return this;
    }

    @Override
    public Optional<ValidationException> validate() {
        if (ids == null || ids.isEmpty()) {
            return Optional.of(ValidationException.withError("trained model id must not be null"));
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetTrainedModelsStatsRequest other = (GetTrainedModelsStatsRequest) o;
        return Objects.equals(ids, other.ids)
            && Objects.equals(allowNoMatch, other.allowNoMatch)
            && Objects.equals(pageParams, other.pageParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ids, allowNoMatch, pageParams);
    }
}
