/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
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
package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;

/**
 * This class is to parse JCommander arguments containing space.
 */
public class ExtendedJCommander extends JCommander {

    ExtendedJCommander(Object object){
        super(object);
    }

    @Override
    public void parse(String... args) {
        if(args[0].equals("add") || args[0].equals("list") || args[0].equals("desc")){
            String[] modifiedCmdArgs = new String[args.length - 1];
            System.arraycopy(args, 2, modifiedCmdArgs,1, modifiedCmdArgs.length - 1);
            modifiedCmdArgs[0] = args[0]+" "+args[1];
            super.parse(modifiedCmdArgs);
        } else{
            super.parse(args);
        }
    }
}
