/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.mockbackend.http2;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Settings;

import static io.netty.handler.logging.LogLevel.INFO;

public final class Http2ClearTextEchoHandlerBuilder
        extends AbstractHttp2ConnectionHandlerBuilder<Http2ClearTextEchoHandler, Http2ClearTextEchoHandlerBuilder> {

    private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, Http2ClearTextEchoHandler.class);

    public Http2ClearTextEchoHandlerBuilder() {
        frameLogger(logger);
    }

    @Override
    public Http2ClearTextEchoHandler build() {
        return super.build();
    }

    @Override
    protected Http2ClearTextEchoHandler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
            Http2Settings initialSettings) {
        Http2ClearTextEchoHandler handler = new Http2ClearTextEchoHandler(decoder, encoder, initialSettings);
        frameListener(handler);
        return handler;
    }
}
