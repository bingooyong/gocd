/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.serverinfo.MessageHandlerForServerInfoRequestProcessor;
import com.thoughtworks.go.plugin.access.common.serverinfo.MessageHandlerForServerInfoRequestProcessor1_0;
import com.thoughtworks.go.plugin.access.common.settings.MessageHandlerForPluginSettingsRequestProcessor;
import com.thoughtworks.go.plugin.access.common.settings.MessageHandlerForPluginSettingsRequestProcessor1_0;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.v1.ElasticAgentExtensionV1;
import com.thoughtworks.go.plugin.access.elastic.v2.ElasticAgentExtensionV2;
import com.thoughtworks.go.plugin.access.elastic.v3.ElasticAgentExtensionV3;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;

@Component
public class ElasticAgentExtension extends AbstractExtension {
    public static final List<String> SUPPORTED_VERSIONS = Arrays.asList(
            ElasticAgentExtensionV1.VERSION, ElasticAgentExtensionV2.VERSION, ElasticAgentExtensionV3.VERSION
    );
    private final Map<String, VersionedElasticAgentExtension> elasticAgentExtensionMap = new HashMap<>();

    @Autowired
    public ElasticAgentExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, ELASTIC_AGENT_EXTENSION), ELASTIC_AGENT_EXTENSION);
        elasticAgentExtensionMap.put(ElasticAgentExtensionV1.VERSION, new ElasticAgentExtensionV1(pluginRequestHelper));
        elasticAgentExtensionMap.put(ElasticAgentExtensionV2.VERSION, new ElasticAgentExtensionV2(pluginRequestHelper));
        elasticAgentExtensionMap.put(ElasticAgentExtensionV3.VERSION, new ElasticAgentExtensionV3(pluginRequestHelper));

        registerHandler(ElasticAgentExtensionV1.VERSION, new PluginSettingsJsonMessageHandler1_0());
        registerHandler(ElasticAgentExtensionV2.VERSION, new PluginSettingsJsonMessageHandler1_0());
        registerHandler(ElasticAgentExtensionV3.VERSION, new PluginSettingsJsonMessageHandler1_0());

        final MessageHandlerForPluginSettingsRequestProcessor1_0 pluginSettingsRequestProcessor = new MessageHandlerForPluginSettingsRequestProcessor1_0();
        final MessageHandlerForServerInfoRequestProcessor1_0 serverInfoRequestProcessor = new MessageHandlerForServerInfoRequestProcessor1_0();

        registerProcessor(ElasticAgentExtensionV1.VERSION, pluginSettingsRequestProcessor, serverInfoRequestProcessor);
        registerProcessor(ElasticAgentExtensionV2.VERSION, pluginSettingsRequestProcessor, serverInfoRequestProcessor);
        registerProcessor(ElasticAgentExtensionV3.VERSION, pluginSettingsRequestProcessor, serverInfoRequestProcessor);
    }

    private void registerProcessor(String version, MessageHandlerForPluginSettingsRequestProcessor pluginSettingsRequestProcessor,
                                   MessageHandlerForServerInfoRequestProcessor serverInfoRequestProcessor) {
        registerMessageHandlerForPluginSettingsRequestProcessor(version, pluginSettingsRequestProcessor);
        registerMessageHandlerForServerInfoRequestProcessor(version, serverInfoRequestProcessor);
    }

    public void createAgent(String pluginId, final String autoRegisterKey, final String environment, final Map<String, String> configuration, JobIdentifier jobIdentifier) {
        getVersionedElasticAgentExtension(pluginId).createAgent(pluginId, autoRegisterKey, environment, configuration, jobIdentifier);
    }

    public void serverPing(final String pluginId) {
        getVersionedElasticAgentExtension(pluginId).serverPing(pluginId);
    }

    public boolean shouldAssignWork(String pluginId, final AgentMetadata agent, final String environment, final Map<String, String> configuration, JobIdentifier identifier) {
        return getVersionedElasticAgentExtension(pluginId).shouldAssignWork(pluginId, agent, environment, configuration, identifier);
    }

    List<PluginConfiguration> getProfileMetadata(String pluginId) {
        return getVersionedElasticAgentExtension(pluginId).getElasticProfileMetadata(pluginId);
    }

    String getProfileView(String pluginId) {
        return getVersionedElasticAgentExtension(pluginId).getElasticProfileView(pluginId);
    }

    public ValidationResult validate(final String pluginId, final Map<String, String> configuration) {
        return getVersionedElasticAgentExtension(pluginId).validateElasticProfile(pluginId, configuration);
    }

    com.thoughtworks.go.plugin.domain.common.Image getIcon(String pluginId) {
        return getVersionedElasticAgentExtension(pluginId).getIcon(pluginId);
    }

    public String getPluginStatusReport(String pluginId) {
        return getVersionedElasticAgentExtension(pluginId).getPluginStatusReport(pluginId);
    }

    public String getAgentStatusReport(String pluginId, JobIdentifier identifier, String elasticAgentId) {
        return getVersionedElasticAgentExtension(pluginId).getAgentStatusReport(pluginId, identifier, elasticAgentId);
    }

    public Capabilities getCapabilities(String pluginId) {
        return getVersionedElasticAgentExtension(pluginId).getCapabilities(pluginId);
    }

    @Override
    protected List<String> goSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }

    protected VersionedElasticAgentExtension getVersionedElasticAgentExtension(String pluginId) {
        final String resolvedExtensionVersion = pluginManager.resolveExtensionVersion(pluginId, ELASTIC_AGENT_EXTENSION, goSupportedVersions());
        return elasticAgentExtensionMap.get(resolvedExtensionVersion);
    }
}
