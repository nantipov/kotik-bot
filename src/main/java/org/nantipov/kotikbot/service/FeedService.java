package org.nantipov.kotikbot.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
@Service
public class FeedService {

    public static final Predicate<SyndEntry> ALL_ENTRIES = entry -> true;

    public static final Predicate<SyndEntry> TODAY_ENTRIES = entry ->
            Optional.ofNullable(entry.getPublishedDate())
                    .or(() -> Optional.ofNullable(entry.getUpdatedDate()))
                    .map(Date::toInstant)
                    .map(instant -> OffsetDateTime.ofInstant(instant, ZoneId.of(ZoneOffset.UTC.getId())))
                    .map(dateTime -> dateTime.truncatedTo(ChronoUnit.DAYS))
                    .filter(dateTime ->
                                    dateTime.isEqual(OffsetDateTime.now(ZoneId.of(ZoneOffset.UTC.getId()))
                                                                   .truncatedTo(ChronoUnit.DAYS))
                    )
                    .isPresent();

    /**
     * Builds a stream of {@code SupplierMessage} from consumed feed items.
     *
     * @param url    url to consume
     * @param filter filter to apply to consumed feed items
     * @param mapper mapper for converting from feed item into {@code SupplierMessage}, if map function returns null,
     *               such items are supposed to be skipped.
     *
     * @return stream of {@code SupplierMessage}
     */
    public Stream<SupplierMessage> feed(String url, Predicate<SyndEntry> filter,
                                        Function<SyndEntry, SupplierMessage> mapper) {
        try (CloseableHttpClient client = HttpClients.createMinimal()) {
            HttpUriRequest request = new HttpGet(url);
            try (var response = client.execute(request);
                 var stream = response.getEntity().getContent()) {
                var input = new SyndFeedInput();
                var feed = input.build(new XmlReader(stream));
                return feed.getEntries()
                           .stream()
                           .filter(filter)
                           .map(mapper)
                           .filter(Objects::nonNull);
            }
        } catch (IOException | FeedException e) {
            log.error("Could not read feed '{}'", url, e);
            return Stream.empty();
        }
    }
}
