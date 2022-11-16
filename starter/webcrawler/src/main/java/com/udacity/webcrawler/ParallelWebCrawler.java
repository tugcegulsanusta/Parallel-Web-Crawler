package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final PageParserFactory parserFactory;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;

  @Inject
  ParallelWebCrawler(
      Clock clock,
      PageParserFactory parserFactory,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @TargetParallelism int threadCount,
      @MaxDepth int maxDepth,
      @IgnoredUrls List<Pattern> ignoredUrls) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    Map<String, Integer> counts = new ConcurrentHashMap<String, Integer>();
    Set<String> visitedUrls = new ConcurrentSkipListSet<String>();
    for (String url : startingUrls) {
      CrawlInternalClass crawlInternalClass = new CrawlInternalClass(url, deadline, maxDepth, counts, visitedUrls);
      pool.invoke(crawlInternalClass);
    }
    if (!counts.isEmpty()) {
      counts=WordCounts.sort(counts, popularWordCount);
    }
    return new CrawlResult.Builder()
            .setWordCounts(counts)
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }

  public class CrawlInternalClass extends RecursiveAction {
    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    public  CrawlInternalClass (String url,
                                Instant deadline,
                                int maxDepth,
                                Map<String, Integer> counts,
                                Set<String> visitedUrls){
      this.url=url;
      this.deadline=deadline;
      this.maxDepth=maxDepth;
      this.counts=counts;
      this.visitedUrls=visitedUrls;
    }
    @Override
    protected void compute() {
      if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
        return;
      }
      for (Pattern pattern : ignoredUrls) {
        if (pattern.matcher(url).matches()) {
          return;
        }
      }
      synchronized (visitedUrls){
        if (visitedUrls.contains(url)) {
          return;
        }
        visitedUrls.add(url);
      }
      PageParser.Result result = parserFactory.get(url).parse();
      synchronized (counts){
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
          if (counts.containsKey(e.getKey())) {
            counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
          } else {
            counts.put(e.getKey(), e.getValue());
          }
        }
      }


      List<CrawlInternalClass> SubRecursiveActions = result.getLinks().stream()
              .map(link -> new CrawlInternalClass(link, deadline, maxDepth - 1, counts, visitedUrls))
              .collect(Collectors.toList());
      invokeAll(SubRecursiveActions);
    }
  }
}
