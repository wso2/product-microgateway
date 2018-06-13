/*
 * Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerina.gateway;

import org.ballerina.gateway.models.APIModel;
import org.ballerina.gateway.models.TierModel;
import org.ballerinalang.compiler.plugins.AbstractCompilerPlugin;
import org.ballerinalang.compiler.plugins.SupportedAnnotationPackages;
import org.ballerinalang.model.tree.AnnotationAttachmentNode;
import org.ballerinalang.model.tree.ResourceNode;
import org.ballerinalang.model.tree.ServiceNode;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;

import java.util.List;

/**
 * Compiler plugin to generate greetings.
 */
@SupportedAnnotationPackages(
        // Tell compiler we are only interested in gateway.tier annotations.
        value = "wso2.gateway"
)

public class GatewayAnnotationPlugin extends AbstractCompilerPlugin {

    public static final String POLICY = "policy";
    public static final String VERSION = "apiVersion";
    public static final String NAME = "name";
    private DiagnosticLog dlog;

    @Override
    public void init(DiagnosticLog diagnosticLog) {
        // Initialize the logger.
        this.dlog = diagnosticLog;
    }

    // Extract annotations attached to resource<> objects
    @Override
    public void process(ResourceNode resourceNode, List<AnnotationAttachmentNode> annotations) {

        //Iterate through the annotation Attachment Node List
        for (AnnotationAttachmentNode attachmentNode : annotations) {
            List<BLangRecordLiteral.BLangRecordKeyValue> keyValues =
                    ((BLangRecordLiteral) ((BLangAnnotationAttachment) attachmentNode).expr).getKeyValuePairs();
            //Iterate through the annotations
            for (BLangRecordLiteral.BLangRecordKeyValue keyValue : keyValues) {
                String annotationValue = keyValue.getValue().toString();
                switch (keyValue.getKey().toString()) {
                //Match annotation key and assign the value to model class
                    case POLICY:
                        TierModel.getInstance().setTier(annotationValue);
                        continue;
                    default:
                        break;
                }
            }
        }
    }

    // Extract annotations attached  to rest<> objects
    @Override
    public void process(ServiceNode serviceNode, List<AnnotationAttachmentNode> annotations) {

        //Iterate through the annotation Attachment Node List
        for (AnnotationAttachmentNode attachmentNode : annotations) {
            List<BLangRecordLiteral.BLangRecordKeyValue> keyValues =
                    ((BLangRecordLiteral) ((BLangAnnotationAttachment) attachmentNode).expr).getKeyValuePairs();
            //Iterate through the annotations
            for (BLangRecordLiteral.BLangRecordKeyValue keyValue : keyValues) {
                String annotationValue = keyValue.getValue().toString();
                switch (keyValue.getKey().toString()) {
                //Match annotation key and assign the value to model class
                case VERSION: {
                    APIModel.getInstance().setApiVersion(annotationValue);
                    continue;
                }
                case NAME: {
                    APIModel.getInstance().setName(annotationValue);
                    continue;
                }
                default:
                    break;
                }
            }
        }
    }
}
