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
