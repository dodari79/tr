package com.opal.torrent.rss.controller;

import com.opal.torrent.rss.model.Quality;
import com.opal.torrent.rss.service.RssService;
import com.rometools.rome.feed.rss.Channel;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.Arrays;

@AllArgsConstructor
@RequestMapping("/rss")
@Controller
public class RssController {

    private final RssService rssService;

    @GetMapping("/feed")
    @ResponseBody
    public Channel getRssFeedBySite(HttpServletRequest request,
                                    @RequestParam String search,
                                    @RequestParam(defaultValue = "1", required = false) int page,
                                    @RequestParam(defaultValue = "1", required = false) int maxPage,
                                    @RequestParam(required = false) String prefer,
                                    @RequestParam(required = false, defaultValue = "0") String startDate,
                                    @RequestParam(required = false, defaultValue = "999999") String endDate,
                                    @RequestParam(required = false) Quality quality) throws URISyntaxException {

        return rssService.getRss(request, search, page, maxPage, prefer, startDate, endDate, quality);
    }

    @GetMapping("/{site}/feed")
    @ResponseBody
    public Channel getRssFeedBySiteAndBoard(HttpServletRequest request,
                                            @PathVariable("site") String site,
                                            @RequestParam(defaultValue = "", required = false) String[] boards,
                                            @RequestParam String search,
                                            @RequestParam(defaultValue = "1", required = false) int page,
                                            @RequestParam(defaultValue = "1", required = false) int maxPage,
                                            @RequestParam(required = false) String prefer,
                                            @RequestParam(required = false, defaultValue = "0") String startDate,
                                            @RequestParam(required = false, defaultValue = "999999") String endDate,
                                            @RequestParam(required = false) Quality quality) throws URISyntaxException {

        return rssService.getRssBySite(request, site, Arrays.asList(boards), search, page, maxPage, prefer, startDate, endDate, quality);
    }

    @GetMapping("/{site}/{board}/{boardId}/down")
    public String download(@PathVariable("site") String site,
                           @PathVariable("board") String board,
                           @PathVariable("boardId") String boardId,
                           @RequestParam(required = false) String prefer) {
        String downloadLink = rssService.getDownloadLink(site, board, boardId, prefer);
        if (StringUtils.isEmpty(downloadLink) == false) {
            return "redirect:" + downloadLink;
        } else {
            System.out.println("site="+ site+", board="+board+", boardId="+boardId +" download link is null");
        }
        return "";
    }
}
