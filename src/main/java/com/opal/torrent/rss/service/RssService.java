package com.opal.torrent.rss.service;

import com.opal.torrent.rss.model.Quality;
import com.opal.torrent.rss.model.TBoard;
import com.opal.torrent.rss.model.TitleLink;
import com.opal.torrent.rss.util.TitleParser;
import com.opal.torrent.rss.util.WebUtil;
import com.rometools.modules.mediarss.MediaModuleImpl;
import com.rometools.modules.mediarss.types.Hash;
import com.rometools.modules.mediarss.types.Metadata;
import com.rometools.rome.feed.module.Module;
import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Enclosure;
import com.rometools.rome.feed.rss.Item;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Service
@AllArgsConstructor
public class RssService {

    public static final String DEFAULT_DATE_VALUE = "870320";
    private final ApplicationContext applicationContext;

    @Cacheable(CacheService.CACHE_NAME_RSS)
    public Channel getRss(HttpServletRequest req, String search, int page, int maxPage, String prefer, String startDate, String endDate, Quality quality) throws URISyntaxException {
        URI reqBaseUri = WebUtil.getRequestBaseUri(req);
        Channel channel = getDefaultChannel();
        channel.setLink(WebUtil.getRequestUrl(req));

        String[] sites = applicationContext.getBeanNamesForType(ITorrentService.class);
        List<Item> itemList = new ArrayList<>();
        Arrays.stream(sites).parallel().forEach(site ->
                itemList.addAll(findAllBySite(reqBaseUri.toString(), site, null, search, page, maxPage, prefer, startDate, endDate, quality)));
        itemList.sort((l, r) -> r.getPubDate().compareTo(l.getPubDate()));

        channel.setItems(itemList);
        return channel;
    }

    @Cacheable(CacheService.CACHE_NAME_RSS_BY_SITE)
    public Channel getRssBySite(HttpServletRequest req, String site, List<String> boards, String search, int page, int maxPage, String prefer, String startDate, String endDate, Quality quality) throws URISyntaxException {
        URI reqBaseUri = WebUtil.getRequestBaseUri(req);
        Channel channel = getDefaultChannel();
        channel.setLink(WebUtil.getRequestUrl(req));
        channel.setItems(findAllBySite(reqBaseUri.toString(), site, boards, search, page, maxPage, prefer, startDate, endDate, quality));
        return channel;
    }

    public String getDownloadLink(String site, String board, String boardId, String prefer) {
        ITorrentService torrentService = applicationContext.getBean(site, ITorrentService.class);
        try {
            Document doc = torrentService.queryView(board, boardId);
            String magnet = torrentService.getMagnet(doc, prefer);
            if (!StringUtils.isEmpty(magnet)) {
                return magnet;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Item> findAllBySite(String reqBaseUri, String site, List<String> boards, String search, int page, int maxPage, String prefer, String startDate, String endDate, Quality quality) {
        List<Item> itemList = new ArrayList<>();
        ITorrentService torrentService = applicationContext.getBean(site, ITorrentService.class);
        Map<String, List<Item>> dateQualityMap = new HashMap<>();
        int startDateInt = 0;
        if (StringUtils.isEmpty(startDate) == false && StringUtil.isNumeric(startDate)) {
            startDateInt = Integer.valueOf(startDate);
        }
        int endDateInt = Integer.MAX_VALUE;
        if (StringUtils.isEmpty(endDate) == false && StringUtil.isNumeric(endDate)) {
            endDateInt = Integer.valueOf(endDate);
        }
        
        try {
            List<Document> docList = torrentService.queryList(boards, search, page, maxPage + 1);
            int finalStartDateInt = startDateInt;
            int finalEndDateInt = endDateInt;
            docList.stream().parallel().forEach(doc -> {
                Elements elements = torrentService.getTableElements(doc);
                elements.forEach(element -> {
                    try {
                        Item item = getItem(reqBaseUri, torrentService, element, prefer, search);
                        if (StringUtils.isEmpty(item.getTitle())) {
                            if (dateQualityMap.containsKey(DEFAULT_DATE_VALUE) == false) {
                                dateQualityMap.put(DEFAULT_DATE_VALUE, new ArrayList<>());

                            }
                            dateQualityMap.get(DEFAULT_DATE_VALUE).add(item);
                            return;
                        }

                        TitleParser.TitleResult titleResult = TitleParser.getTitleResult(item.getTitle());
                        if (StringUtils.isEmpty(titleResult.getProfile()) == false) {
                            item.setComments(titleResult.getProfile());
                        }
                        if (titleResult.getDate() >= finalStartDateInt && titleResult.getDate() <= finalEndDateInt) {
                            String dateStr = String.valueOf(titleResult.getDate());
                            if (dateQualityMap.containsKey(dateStr) == false) {
                                dateQualityMap.put(dateStr, new ArrayList<>());

                            }
                            dateQualityMap.get(dateStr).add(item);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, List<Item>> entry : dateQualityMap.entrySet()) {
            if (entry.getValue().size() > 1 && quality != null) {
                Optional<Item> first = entry.getValue().stream().filter(item -> quality.getDesc().equals(item.getComments()))
                        .findFirst();
                if (first.isPresent()) {
                    itemList.add(first.get());
                } else {
                    itemList.addAll(entry.getValue());
                }
            } else {
                itemList.addAll(entry.getValue());
            }
        }

        itemList.sort((l, r) -> r.getPubDate().compareTo(l.getPubDate()));
        return itemList;
    }

    private Item getItem(String reqBaseUri, ITorrentService torrentService, Element element, String prefer, String search) throws URISyntaxException {
        Item item = new Item();
        TitleLink titleLink = torrentService.getTitleAndLink(element, search);
        item.setTitle(titleLink.getTitle());
        item.setLink(titleLink.getLink());

        TBoard board = torrentService.getBoard(item.getLink());
        item.setCategories(torrentService.getCategory(element, board.getName()));
        item.setPubDate(torrentService.getDate(element));

        String downloadUrl = String.format("%s/rss/%s/%s/%s/down", reqBaseUri, torrentService.getSiteSimpleName(), board.getName(), board.getId());
        if (!StringUtils.isEmpty(prefer)) {
            downloadUrl += String.format("?prefer=%s", prefer);
        }
        Enclosure enclosure = new Enclosure();
        enclosure.setUrl(downloadUrl);
        enclosure.setLength(torrentService.getFileSize(element));
        item.setEnclosures(Collections.singletonList(enclosure));

        item.setModules(getModules(downloadUrl));

        return item;
    }

    private List<Module> getModules(String downloadUrl) {
        MediaModuleImpl mediaModule = new MediaModuleImpl();
        Metadata metadata = new Metadata();
        metadata.setHash(new Hash("md5", DigestUtils.md5Hex(downloadUrl)));
        mediaModule.setMetadata(metadata);
        return Collections.singletonList(mediaModule);
    }

    private Channel getDefaultChannel() {
        Channel channel = new Channel();
        channel.setFeedType("rss_2.0");
        channel.setTitle("Torrent RSS Feed");
        channel.setDescription("Torrent RSS feed");
        channel.setGenerator("stkang90");
        channel.setLanguage("ko-kr");
        channel.setPubDate(new Date());
        return channel;
    }
}
