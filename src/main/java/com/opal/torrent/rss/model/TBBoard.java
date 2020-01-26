package com.opal.torrent.rss.model;

import lombok.Getter;

@Getter
public enum TBBoard {
    ko_movie("한국영화"),
    movie("해외영화"),
    drama("한국드라마"),
    en_drama("해외드라마"),
    ent("예능"),
    tv("TV"),
    kids("어린이"),
    ani("애니"),
    ko_music("음악");

    private String name;

    TBBoard(String name) {
        this.name = name;
    }
}
