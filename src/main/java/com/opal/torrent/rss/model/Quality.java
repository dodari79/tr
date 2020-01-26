package com.opal.torrent.rss.model;

import lombok.Getter;

@Getter
public enum Quality {
    HD("720p"), FHD("1080p");

    private String desc;

    Quality(String desc) {
        this.desc = desc;
    }
}
