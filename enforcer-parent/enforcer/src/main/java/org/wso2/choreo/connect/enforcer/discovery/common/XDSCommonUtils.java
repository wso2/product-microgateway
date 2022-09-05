/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.discovery.common;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.envoyproxy.envoy.config.core.v3.Node;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.AdapterConstants;

/**
 * Common utility functions used by XDS clients.
 */
public class XDSCommonUtils {
    private static final Struct nodeMetadata = generateNodeMetadata();

    public static Node generateXDSNode(String nodeId) {
        return Node.newBuilder().setId(nodeId).setMetadata(nodeMetadata).build();
    }

    private static Struct generateNodeMetadata() {
        return Struct.newBuilder().putFields(AdapterConstants.NODE_IDENTIFIER_KEY,
                Value.newBuilder().setStringValue(ConfigHolder.getInstance().getEnvVarConfig().getInstanceIdentifier())
                        .build())
                .build();
    }
}
