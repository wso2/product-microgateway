/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package logging

import "github.com/sirupsen/logrus"

const (
    //LOG_FILE_NAME = "resources/logs/microgateway.log"
    DEFAULT_LOG_LEVEL = logrus.WarnLevel

    //log levels
	LEVEL_PANIC = "PANC"
	LEVEL_FATAL = "FATL"
	LEVEL_ERROR = "ERRO"
	LEVEL_WARN  = "WARN"
	LEVEL_INFO  = "INFO"
	LEVEL_DEBUG = "DEBG"
)
