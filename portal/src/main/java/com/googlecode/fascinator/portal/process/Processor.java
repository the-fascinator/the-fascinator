package com.googlecode.fascinator.portal.process;

import com.googlecode.fascinator.common.JsonSimple;
import java.util.HashMap;

public interface Processor {
    public boolean process(String id, String inputKey,String outputKey, String stage, String configFilePath, HashMap dataMap) throws Exception;
}