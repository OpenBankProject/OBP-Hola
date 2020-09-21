package com.openban.hydra.auth.VO;

public class AccessToViewRequest {
    private String[] views;

    public AccessToViewRequest(String[] views) {
        this.views = views;
    }

    public String[] getViews() {
        return views;
    }

    public void setViews(String[] views) {
        this.views = views;
    }
}
