/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.transform;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.util.SetOnce;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksResponse;
import org.elasticsearch.action.admin.cluster.snapshots.features.ResetFeatureStateResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.IndexTemplateMetadata;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.NamedXContentRegistry.Entry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.indices.SystemIndexDescriptor;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.persistent.PersistentTasksExecutor;
import org.elasticsearch.plugins.PersistentTaskPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SystemIndexPlugin;
import org.elasticsearch.repositories.RepositoriesService;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ExecutorBuilder;
import org.elasticsearch.threadpool.FixedExecutorBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.core.XPackPlugin;
import org.elasticsearch.xpack.core.action.SetResetModeActionRequest;
import org.elasticsearch.xpack.core.action.XPackInfoFeatureAction;
import org.elasticsearch.xpack.core.action.XPackUsageFeatureAction;
import org.elasticsearch.xpack.core.scheduler.SchedulerEngine;
import org.elasticsearch.xpack.core.transform.TransformField;
import org.elasticsearch.xpack.core.transform.TransformMessages;
import org.elasticsearch.xpack.core.transform.TransformNamedXContentProvider;
import org.elasticsearch.xpack.core.transform.action.DeleteTransformAction;
import org.elasticsearch.xpack.core.transform.action.GetTransformAction;
import org.elasticsearch.xpack.core.transform.action.GetTransformStatsAction;
import org.elasticsearch.xpack.core.transform.action.PreviewTransformAction;
import org.elasticsearch.xpack.core.transform.action.PutTransformAction;
import org.elasticsearch.xpack.core.transform.action.SetResetModeAction;
import org.elasticsearch.xpack.core.transform.action.StartTransformAction;
import org.elasticsearch.xpack.core.transform.action.StopTransformAction;
import org.elasticsearch.xpack.core.transform.action.UpdateTransformAction;
import org.elasticsearch.xpack.core.transform.action.compat.DeleteTransformActionDeprecated;
import org.elasticsearch.xpack.core.transform.action.compat.GetTransformActionDeprecated;
import org.elasticsearch.xpack.core.transform.action.compat.GetTransformStatsActionDeprecated;
import org.elasticsearch.xpack.core.transform.action.compat.PreviewTransformActionDeprecated;
import org.elasticsearch.xpack.core.transform.action.compat.PutTransformActionDeprecated;
import org.elasticsearch.xpack.core.transform.action.compat.StartTransformActionDeprecated;
import org.elasticsearch.xpack.core.transform.action.compat.StopTransformActionDeprecated;
import org.elasticsearch.xpack.core.transform.action.compat.UpdateTransformActionDeprecated;
import org.elasticsearch.xpack.core.transform.transforms.persistence.TransformInternalIndexConstants;
import org.elasticsearch.xpack.transform.action.TransportDeleteTransformAction;
import org.elasticsearch.xpack.transform.action.TransportGetTransformAction;
import org.elasticsearch.xpack.transform.action.TransportGetTransformStatsAction;
import org.elasticsearch.xpack.transform.action.TransportPreviewTransformAction;
import org.elasticsearch.xpack.transform.action.TransportPutTransformAction;
import org.elasticsearch.xpack.transform.action.TransportSetTransformResetModeAction;
import org.elasticsearch.xpack.transform.action.TransportStartTransformAction;
import org.elasticsearch.xpack.transform.action.TransportStopTransformAction;
import org.elasticsearch.xpack.transform.action.TransportUpdateTransformAction;
import org.elasticsearch.xpack.transform.action.compat.TransportDeleteTransformActionDeprecated;
import org.elasticsearch.xpack.transform.action.compat.TransportGetTransformActionDeprecated;
import org.elasticsearch.xpack.transform.action.compat.TransportGetTransformStatsActionDeprecated;
import org.elasticsearch.xpack.transform.action.compat.TransportPreviewTransformActionDeprecated;
import org.elasticsearch.xpack.transform.action.compat.TransportPutTransformActionDeprecated;
import org.elasticsearch.xpack.transform.action.compat.TransportStartTransformActionDeprecated;
import org.elasticsearch.xpack.transform.action.compat.TransportStopTransformActionDeprecated;
import org.elasticsearch.xpack.transform.action.compat.TransportUpdateTransformActionDeprecated;
import org.elasticsearch.xpack.transform.checkpoint.TransformCheckpointService;
import org.elasticsearch.xpack.transform.notifications.TransformAuditor;
import org.elasticsearch.xpack.transform.persistence.IndexBasedTransformConfigManager;
import org.elasticsearch.xpack.transform.persistence.TransformConfigManager;
import org.elasticsearch.xpack.transform.persistence.TransformInternalIndex;
import org.elasticsearch.xpack.transform.rest.action.RestCatTransformAction;
import org.elasticsearch.xpack.transform.rest.action.RestDeleteTransformAction;
import org.elasticsearch.xpack.transform.rest.action.RestGetTransformAction;
import org.elasticsearch.xpack.transform.rest.action.RestGetTransformStatsAction;
import org.elasticsearch.xpack.transform.rest.action.RestPreviewTransformAction;
import org.elasticsearch.xpack.transform.rest.action.RestPutTransformAction;
import org.elasticsearch.xpack.transform.rest.action.RestStartTransformAction;
import org.elasticsearch.xpack.transform.rest.action.RestStopTransformAction;
import org.elasticsearch.xpack.transform.rest.action.RestUpdateTransformAction;
import org.elasticsearch.xpack.transform.rest.action.compat.RestDeleteTransformActionDeprecated;
import org.elasticsearch.xpack.transform.rest.action.compat.RestGetTransformActionDeprecated;
import org.elasticsearch.xpack.transform.rest.action.compat.RestGetTransformStatsActionDeprecated;
import org.elasticsearch.xpack.transform.rest.action.compat.RestPreviewTransformActionDeprecated;
import org.elasticsearch.xpack.transform.rest.action.compat.RestPutTransformActionDeprecated;
import org.elasticsearch.xpack.transform.rest.action.compat.RestStartTransformActionDeprecated;
import org.elasticsearch.xpack.transform.rest.action.compat.RestStopTransformActionDeprecated;
import org.elasticsearch.xpack.transform.rest.action.compat.RestUpdateTransformActionDeprecated;
import org.elasticsearch.xpack.transform.transforms.TransformPersistentTasksExecutor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static org.elasticsearch.xpack.core.transform.TransformMessages.FAILED_TO_UNSET_RESET_MODE;
import static org.elasticsearch.xpack.core.transform.transforms.persistence.TransformInternalIndexConstants.AUDIT_INDEX_PATTERN;
import static org.elasticsearch.xpack.core.transform.transforms.persistence.TransformInternalIndexConstants.TRANSFORM_PREFIX;
import static org.elasticsearch.xpack.core.transform.transforms.persistence.TransformInternalIndexConstants.TRANSFORM_PREFIX_DEPRECATED;

public class Transform extends Plugin implements SystemIndexPlugin, PersistentTaskPlugin {

    public static final String NAME = "transform";
    public static final String TASK_THREAD_POOL_NAME = "transform_indexing";

    private static final Logger logger = LogManager.getLogger(Transform.class);

    private final Settings settings;
    private final SetOnce<TransformServices> transformServices = new SetOnce<>();

    public static final int DEFAULT_FAILURE_RETRIES = 10;
    public static final Integer DEFAULT_INITIAL_MAX_PAGE_SEARCH_SIZE = Integer.valueOf(500);
    public static final TimeValue DEFAULT_TRANSFORM_FREQUENCY = TimeValue.timeValueMillis(60000);

    // How many times the transform task can retry on an non-critical failure
    public static final Setting<Integer> NUM_FAILURE_RETRIES_SETTING = Setting.intSetting(
        "xpack.transform.num_transform_failure_retries",
        DEFAULT_FAILURE_RETRIES,
        0,
        100,
        Setting.Property.NodeScope,
        Setting.Property.Dynamic
    );

    public Transform(Settings settings) {
        this.settings = settings;
    }

    protected XPackLicenseState getLicenseState() {
        return XPackPlugin.getSharedLicenseState();
    }

    @Override
    public List<RestHandler> getRestHandlers(
        final Settings settings,
        final RestController restController,
        final ClusterSettings clusterSettings,
        final IndexScopedSettings indexScopedSettings,
        final SettingsFilter settingsFilter,
        final IndexNameExpressionResolver indexNameExpressionResolver,
        final Supplier<DiscoveryNodes> nodesInCluster
    ) {

        return Arrays.asList(
            new RestPutTransformAction(),
            new RestStartTransformAction(),
            new RestStopTransformAction(),
            new RestDeleteTransformAction(),
            new RestGetTransformAction(),
            new RestGetTransformStatsAction(),
            new RestPreviewTransformAction(),
            new RestUpdateTransformAction(),
            new RestCatTransformAction(),

            // deprecated endpoints, to be removed for 8.0.0
            new RestPutTransformActionDeprecated(),
            new RestStartTransformActionDeprecated(),
            new RestStopTransformActionDeprecated(),
            new RestDeleteTransformActionDeprecated(),
            new RestGetTransformActionDeprecated(),
            new RestGetTransformStatsActionDeprecated(),
            new RestPreviewTransformActionDeprecated(),
            new RestUpdateTransformActionDeprecated()
        );
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {

        return Arrays.asList(
            new ActionHandler<>(PutTransformAction.INSTANCE, TransportPutTransformAction.class),
            new ActionHandler<>(StartTransformAction.INSTANCE, TransportStartTransformAction.class),
            new ActionHandler<>(StopTransformAction.INSTANCE, TransportStopTransformAction.class),
            new ActionHandler<>(DeleteTransformAction.INSTANCE, TransportDeleteTransformAction.class),
            new ActionHandler<>(GetTransformAction.INSTANCE, TransportGetTransformAction.class),
            new ActionHandler<>(GetTransformStatsAction.INSTANCE, TransportGetTransformStatsAction.class),
            new ActionHandler<>(PreviewTransformAction.INSTANCE, TransportPreviewTransformAction.class),
            new ActionHandler<>(UpdateTransformAction.INSTANCE, TransportUpdateTransformAction.class),
            new ActionHandler<>(SetResetModeAction.INSTANCE, TransportSetTransformResetModeAction.class),

            // deprecated actions, to be removed for 8.0.0
            new ActionHandler<>(PutTransformActionDeprecated.INSTANCE, TransportPutTransformActionDeprecated.class),
            new ActionHandler<>(StartTransformActionDeprecated.INSTANCE, TransportStartTransformActionDeprecated.class),
            new ActionHandler<>(StopTransformActionDeprecated.INSTANCE, TransportStopTransformActionDeprecated.class),
            new ActionHandler<>(DeleteTransformActionDeprecated.INSTANCE, TransportDeleteTransformActionDeprecated.class),
            new ActionHandler<>(GetTransformActionDeprecated.INSTANCE, TransportGetTransformActionDeprecated.class),
            new ActionHandler<>(GetTransformStatsActionDeprecated.INSTANCE, TransportGetTransformStatsActionDeprecated.class),
            new ActionHandler<>(PreviewTransformActionDeprecated.INSTANCE, TransportPreviewTransformActionDeprecated.class),
            new ActionHandler<>(UpdateTransformActionDeprecated.INSTANCE, TransportUpdateTransformActionDeprecated.class),

            // usage and info
            new ActionHandler<>(XPackUsageFeatureAction.TRANSFORM, TransformUsageTransportAction.class),
            new ActionHandler<>(XPackInfoFeatureAction.TRANSFORM, TransformInfoTransportAction.class)
        );
    }

    @Override
    public List<ExecutorBuilder<?>> getExecutorBuilders(Settings settings) {
        FixedExecutorBuilder indexing = new FixedExecutorBuilder(
            settings,
            TASK_THREAD_POOL_NAME,
            4,
            4,
            "transform.task_thread_pool",
            false
        );

        return Collections.singletonList(indexing);
    }

    @Override
    public Collection<Object> createComponents(
        Client client,
        ClusterService clusterService,
        ThreadPool threadPool,
        ResourceWatcherService resourceWatcherService,
        ScriptService scriptService,
        NamedXContentRegistry xContentRegistry,
        Environment environment,
        NodeEnvironment nodeEnvironment,
        NamedWriteableRegistry namedWriteableRegistry,
        IndexNameExpressionResolver expressionResolver,
        Supplier<RepositoriesService> repositoriesServiceSupplier
    ) {
        TransformConfigManager configManager = new IndexBasedTransformConfigManager(client, xContentRegistry);
        TransformAuditor auditor = new TransformAuditor(client, clusterService.getNodeName(), clusterService);
        TransformCheckpointService checkpointService = new TransformCheckpointService(settings, clusterService, configManager, auditor);
        SchedulerEngine scheduler = new SchedulerEngine(settings, Clock.systemUTC());

        transformServices.set(new TransformServices(configManager, checkpointService, auditor, scheduler));

        return Arrays.asList(transformServices.get(), new TransformClusterStateListener(clusterService, client));
    }

    @Override
    public UnaryOperator<Map<String, IndexTemplateMetadata>> getIndexTemplateMetadataUpgrader() {
        return templates -> {
            try {
                templates.put(TransformInternalIndexConstants.AUDIT_INDEX, TransformInternalIndex.getAuditIndexTemplateMetadata());
            } catch (IOException e) {
                logger.warn("Error creating transform audit index", e);
            }
            return templates;
        };
    }

    @Override
    public List<PersistentTasksExecutor<?>> getPersistentTasksExecutor(
        ClusterService clusterService,
        ThreadPool threadPool,
        Client client,
        SettingsModule settingsModule,
        IndexNameExpressionResolver expressionResolver
    ) {
        // the transform services should have been created
        assert transformServices.get() != null;

        return Collections.singletonList(
            new TransformPersistentTasksExecutor(
                client,
                transformServices.get(),
                threadPool,
                clusterService,
                settingsModule.getSettings(),
                expressionResolver
            )
        );
    }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(NUM_FAILURE_RETRIES_SETTING);
    }

    @Override
    public void close() {
        if (transformServices.get() != null) {
            transformServices.get().getSchedulerEngine().stop();
        }
    }

    @Override
    public List<Entry> getNamedXContent() {
        return new TransformNamedXContentProvider().getNamedXContentParsers();
    }

    @Override
    public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
        try {
            return Collections.singletonList(TransformInternalIndex.getSystemIndexDescriptor());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override public Collection<String> getAssociatedIndexPatterns() {
        return List.of(AUDIT_INDEX_PATTERN);
    }

    @Override
    public void cleanUpFeature(
        ClusterService clusterService,
        Client client,
        ActionListener<ResetFeatureStateResponse.ResetFeatureStateStatus> finalListener
    ) {

        ActionListener<ResetFeatureStateResponse.ResetFeatureStateStatus> unsetResetModeListener = ActionListener.wrap(
            success -> client.execute(SetResetModeAction.INSTANCE, SetResetModeActionRequest.disabled(), ActionListener.wrap(
                resetSuccess -> finalListener.onResponse(success),
                resetFailure -> {
                    logger.error("failed to disable reset mode after otherwise successful transform reset", resetFailure);
                    finalListener.onFailure(
                        new ElasticsearchStatusException(
                            TransformMessages.getMessage(FAILED_TO_UNSET_RESET_MODE, "a successful feature reset"),
                            RestStatus.INTERNAL_SERVER_ERROR,
                            resetFailure
                        )
                    );
                })
            ),
            failure -> client.execute(SetResetModeAction.INSTANCE, SetResetModeActionRequest.disabled(), ActionListener.wrap(
                resetSuccess -> finalListener.onFailure(failure),
                resetFailure -> {
                    logger.error(
                        TransformMessages.getMessage(FAILED_TO_UNSET_RESET_MODE, "a failed feature reset"),
                        resetFailure
                    );
                    Exception ex = new ElasticsearchException(
                        TransformMessages.getMessage(FAILED_TO_UNSET_RESET_MODE, "a failed feature reset")
                    );
                    ex.addSuppressed(resetFailure);
                    failure.addSuppressed(ex);
                    finalListener.onFailure(failure);
                })
            )
        );

        ActionListener<ListTasksResponse> afterWaitingForTasks = ActionListener.wrap(
            listTasksResponse -> {
                listTasksResponse.rethrowFailures("Waiting for transform indexing tasks");
                SystemIndexPlugin.super.cleanUpFeature(clusterService, client, unsetResetModeListener);
            },
            unsetResetModeListener::onFailure
        );

        ActionListener<StopTransformAction.Response> afterStoppingTransforms = ActionListener.wrap(stopTransformsResponse -> {
            if (stopTransformsResponse.isAcknowledged()
                && stopTransformsResponse.getTaskFailures().isEmpty()
                && stopTransformsResponse.getNodeFailures().isEmpty()) {
                client.admin()
                    .cluster()
                    .prepareListTasks()
                    .setActions(TransformField.TASK_NAME)
                    .setWaitForCompletion(true)
                    .execute(ActionListener.wrap(
                        listTransformTasks -> {
                            listTransformTasks.rethrowFailures("Waiting for transform tasks");
                            client.admin()
                                .cluster()
                                .prepareListTasks()
                                .setActions("indices:data/write/bulk")
                                .setDetailed(true)
                                .setWaitForCompletion(true)
                                .setDescriptions("*" + TRANSFORM_PREFIX + "*", "*" + TRANSFORM_PREFIX_DEPRECATED + "*")
                                .execute(afterWaitingForTasks);
                        },
                        unsetResetModeListener::onFailure
                    ));
            } else {
                String errMsg = "Failed to reset Transform: "
                    + (stopTransformsResponse.isAcknowledged() ? "" : "not acknowledged ")
                    + (stopTransformsResponse.getNodeFailures().isEmpty()
                        ? ""
                        : "node failures: " + stopTransformsResponse.getNodeFailures() + " ")
                    + (stopTransformsResponse.getTaskFailures().isEmpty()
                        ? ""
                        : "task failures: " + stopTransformsResponse.getTaskFailures());
                unsetResetModeListener.onResponse(new ResetFeatureStateResponse.ResetFeatureStateStatus(this.getFeatureName(), errMsg));
            }
        }, unsetResetModeListener::onFailure);

        ActionListener<AcknowledgedResponse> afterResetModeSet = ActionListener.wrap(
            response -> {
                StopTransformAction.Request stopTransformsRequest = new StopTransformAction.Request(
                    Metadata.ALL,
                    true,
                    true,
                    null,
                    true,
                    false
                );
                client.execute(StopTransformAction.INSTANCE, stopTransformsRequest, afterStoppingTransforms);
            },
            finalListener::onFailure
        );

        client.execute(SetResetModeAction.INSTANCE, SetResetModeActionRequest.enabled(), afterResetModeSet);
    }

    @Override
    public String getFeatureName() {
        return "transform";
    }

    @Override
    public String getFeatureDescription() {
        return "Manages configuration and state for transforms";
    }
}
