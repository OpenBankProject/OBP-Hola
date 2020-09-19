package com.openban.hydra.auth.VO;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.ArrayUtils;

public class AccessToViewRequest {
    @JsonProperty("grant_views")
    private String[] grant_views;
    @JsonProperty("revoke_views")
    private String[] revoke_views;

    public AccessToViewRequest(String[] allViews, String[] grant_views) {
        this.grant_views = grant_views;
        this.revoke_views = ArrayUtils.removeElements(allViews, grant_views);
    }
    public AccessToViewRequest(String[] allViews) {
        this.grant_views = new String[]{};
        this.revoke_views = allViews;
    }
}
