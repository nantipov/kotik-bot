package org.nantipov.kotikbot.service.updatesupplier;

import com.google.common.base.Charsets;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.PseudoTextElement;
import org.jsoup.select.Elements;
import org.nantipov.kotikbot.domain.MessageResource;
import org.nantipov.kotikbot.domain.MessageResourceType;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.nantipov.kotikbot.service.FeedService;
import org.nantipov.kotikbot.service.UpdatesService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.nullToEmpty;
import static org.nantipov.kotikbot.service.Utils.escapeReservedCharacters;

@Slf4j
@Component
public class WikipediaSupplier implements UpdateSupplier {

    private static final String SUPPLIER_ID = "WikipediaSupplier";

    private static final String WIKI_ATOM_POD =
            "https://commons.wikimedia.org/w/api.php?action=featuredfeed&feed=potd&feedformat=atom&language=en";
    private static final String WIKI_BASE_URL = "https://commons.wikimedia.org";
    private static final String POD_DESCRIPTION_TYPE = "html";

    private final UpdatesService updatesService;
    private final FeedService feedService;

    public WikipediaSupplier(UpdatesService updatesService, FeedService feedService) {
        this.updatesService = updatesService;
        this.feedService = feedService;
    }

    @Override
    public void deliver() {
        deliverPictureOfTheDay();
    }

    private void deliverPictureOfTheDay() {
        var today = OffsetDateTime.now(ZoneId.of(ZoneOffset.UTC.getId())).truncatedTo(ChronoUnit.DAYS);
        String updateKey = "POD" + DateTimeFormatter.BASIC_ISO_DATE.format(today);
        if (updatesService.isAlreadyDelivered(SUPPLIER_ID, updateKey)) {
            return;
        }
        feedService.feed(WIKI_ATOM_POD, FeedService.TODAY_ENTRIES, this::podMessage)
                   .findAny()
                   .ifPresent(message ->
                                      updatesService.storeUpdate(
                                              SUPPLIER_ID, updateKey, today.plusHours(23).plusMinutes(59), message
                                      )
                   );
    }

    private SupplierMessage podMessage(SyndEntry syndEntry) {
        var description = syndEntry.getDescription();
        if (description != null && POD_DESCRIPTION_TYPE.equals(description.getType())) {
            var doc = Jsoup.parseBodyFragment(description.getValue());
            var message = new SupplierMessage();
            message.setMarkdownText(
                    "* Wikipedia :: _Picture Of The Day_ :: " +
                    escapeReservedCharacters(DateTimeFormatter.ISO_LOCAL_DATE.format(OffsetDateTime.now())) + " *"
            );
            addPODDescription(message, doc);
            message.setResources(
                    doc.select("a[href*=/wiki/File:]")
                       .stream()
                       .map(element -> element.attr("href"))
                       .flatMap(this::wikiFilePageToResource)
                       .collect(Collectors.toList())
            );
            return message;
        }
        return null;
    }

    private Stream<MessageResource> wikiFilePageToResource(String url) {
        String fullUrl = WIKI_BASE_URL + url;
        try (CloseableHttpClient client = HttpClients.createMinimal()) {
            HttpUriRequest request = new HttpGet(fullUrl);
            try (var response = client.execute(request);
                 var stream = response.getEntity().getContent()) {
                var doc = Jsoup.parse(stream, Charsets.UTF_8.name(), url);
                return doc.select("a[href*=upload.wikimedia.org].mw-thumbnail-link")
                          .stream()
                          .filter(a -> getThumbnailSize(a.text()) <= 2500)
                          .sorted(Comparator.<Element>comparingInt(e -> getThumbnailSize(e.text())).reversed())
                          .limit(1)
                          .map(a -> new MessageResource(
                                  MessageResourceType.IMAGE,
                                  a.attr("href"),
                                  escapeReservedCharacters(nullToEmpty(a.text())))
                          );
            }
        } catch (IOException e) {
            log.error("Could not read Wiki File: page '{}'", url, e);
            return Stream.empty();
        }
    }

    private int getThumbnailSize(String thumbnailText) {
        if (thumbnailText != null && thumbnailText.length() > 2 && Character.isDigit(thumbnailText.charAt(0))) {
            var resolutionXPos = thumbnailText.toLowerCase()
                                              .indexOf(' ');
            if (resolutionXPos > -1) {
                try {
                    return Integer.parseInt(
                            thumbnailText.substring(0, resolutionXPos)
                                         .replaceAll("\\D", "")
                                         .trim()
                    );
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private void addPODDescription(SupplierMessage message, Document document) {
        message.setMarkdownText(
                message.getMarkdownText() + " \n\n" +
                document.select("div.description")
                        .stream()
                        .map(this::htmlToMarkdown)
                        .collect(Collectors.joining(" "))
        );
    }

    private String htmlToMarkdown(Element element) {
        if (element instanceof PseudoTextElement) {
            return escapeReservedCharacters(element.text());
        } else {
            switch (element.tag().getName().toLowerCase()) {
                case "a":
                    return String.format(" [%s](%s)", escapeReservedCharacters(element.text()),
                                         escapeReservedCharacters(element.attr("href")));
                case "b":
                    return "*" + escapeReservedCharacters(nullToEmpty(element.text())) +
                           htmlToMarkdown(element.children()) + "*";
                case "i":
                    return "_" + escapeReservedCharacters(nullToEmpty(element.text())) +
                           htmlToMarkdown(element.children()) + "_";
                default:
                    return escapeReservedCharacters(nullToEmpty(element.text())) + htmlToMarkdown(element.children());
            }
        }
    }

    private String htmlToMarkdown(Elements elements) {
        if (!elements.isEmpty()) {
            return elements.stream()
                           .map(this::htmlToMarkdown)
                           .collect(Collectors.joining(" "));
        } else {
            return "";
        }
    }
}
