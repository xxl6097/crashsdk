package com.crashsdk.net;

import android.text.TextUtils;

import com.crashsdk.callback.OnEmailEvents;

import java.io.Serializable;

public class EmailBean implements Serializable {
    private OnEmailEvents onEmailEvents;
    private Object email;
    private String fromAddr = "customoncrash@163.com";//from.getText().toString();
    private String password = "xmrehtsjaxouzdbi";//客户端授权密码
    private String toAddr = "263996097@qq.com";

    public String getFromAddr() {
        return fromAddr;
    }

    public void setFromAddr(String fromAddr) {
        this.fromAddr = fromAddr;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToAddr() {
        return toAddr;
    }

    public void setToAddr(String toAddr) {
        if (TextUtils.isEmpty(toAddr))
            return;
        this.toAddr = toAddr;
    }

    public OnEmailEvents getOnEmailEvents() {
        return onEmailEvents;
    }

    public void setOnEmailEvents(OnEmailEvents onEmailEvents) {
        this.onEmailEvents = onEmailEvents;
    }

    public Object getEmail() {
        return email;
    }

    public void setEmail(Object email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "EmailBean{" +
                "onEmailEvents=" + onEmailEvents +
                ", email=" + email +
                ", fromAddr='" + fromAddr + '\'' +
                ", password='" + password + '\'' +
                ", toAddr='" + toAddr + '\'' +
                '}';
    }
}
