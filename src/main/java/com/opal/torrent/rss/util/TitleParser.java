package com.opal.torrent.rss.util;

import com.opal.torrent.rss.model.Quality;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jsoup.helper.StringUtil;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by DoYoungKim on 2019.03.25..
 */
public class TitleParser {

    public static TitleResult getTitleResult(String title, Date pubDate) {
        if (StringUtils.isEmpty(title)) {
            return null;
        }

        String[] split = title.trim().split("\\.");
        String profile = null;
        Integer date = null;
        for (String keyword : split) {
            if (keyword.toLowerCase().indexOf(Quality.HD.getDesc()) > -1) {
                profile = Quality.HD.getDesc();
            } else if (keyword.toLowerCase().indexOf(Quality.FHD.getDesc()) > -1) {
                profile = Quality.FHD.getDesc();
            }

            if (date == null && StringUtil.isNumeric(keyword) && (keyword.length() == 6 || keyword.length() == 8)) {
                keyword = keyword.length() == 8 ? keyword.substring(2) : keyword;
                date = Integer.valueOf(keyword);
            }
        }
        if (date == null && pubDate != null){
            SimpleDateFormat transFormat = new SimpleDateFormat("yyyyMMdd");
            String to = transFormat.format(pubDate);
            date = Integer.valueOf(to.substring(2));
        }
        return TitleResult.of(profile, date);
    }

    @Getter
    @AllArgsConstructor(staticName = "of")
    public static class TitleResult {
        private String profile;
        private int date;
    }
}
