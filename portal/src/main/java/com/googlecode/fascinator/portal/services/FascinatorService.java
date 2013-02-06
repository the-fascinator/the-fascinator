package com.googlecode.fascinator.portal.services;

import com.googlecode.fascinator.common.JsonSimple;

public interface FascinatorService {
    public JsonSimple getConfig();

    public void setConfig(JsonSimple config);

    public void init();
}