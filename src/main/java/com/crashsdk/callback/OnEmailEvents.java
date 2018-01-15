package com.crashsdk.callback;

/**
 * Created by Android Studio.
 * Author: uuxia
 * Date: 2015-12-31 10:01
 * Description:
 */
/*
 * -----------------------------------------------------------------
 * Copyright ?2014 clife
 * Shenzhen H&T Intelligent Control Co.,Ltd.
 * -----------------------------------------------------------------
 *
 * File: OnEmailEvents.java
 * Create: 2015/12/31 10:01
 */
public abstract class OnEmailEvents {
    private String taskIsClassName;

    public String getTaskIsClassName() {
        return taskIsClassName;
    }

    public void setTaskIsClassName(String taskIsClassName) {
        this.taskIsClassName = taskIsClassName;
    }

    public OnEmailEvents(String id) {
        this.taskIsClassName = id;
    }

    public abstract void sendSucessfull(String taskIsClassName);
    public abstract void sendFaid(String taskIsClassName);
}
