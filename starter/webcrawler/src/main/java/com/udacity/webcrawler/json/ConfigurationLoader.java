package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

  private final Path path;

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   */
  public ConfigurationLoader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   */
  public CrawlerConfiguration load() {
    // DONE: read JSON string from a file Path, pass to string to the reader. Don't forget to close the file.
   try {
     BufferedReader reader = Files.newBufferedReader(this.path,StandardCharsets.UTF_8);
     reader.close();
     return read(reader);

   }catch (IOException e){
       throw new RuntimeException(e);
   }
  }
  /**
   * Loads crawler configuration from the given reader.
   *
   * @param reader a Reader pointing to a JSON string that contains crawler configuration.
   * @return a crawler configuration
   */
  public static CrawlerConfiguration read(Reader reader){
    Objects.requireNonNull(reader);// This is here to get rid of the unused variable warning.
    // DONE: read JSON input and parse it into CrawlerConfiguration + use JSON library.
    ObjectMapper objectMapper=new ObjectMapper();
    objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
      try {
          CrawlerConfiguration.Builder  crawlerConfigurationBuilder = objectMapper.readValue(reader, CrawlerConfiguration.Builder.class);
          CrawlerConfiguration crawlerConfiguration = crawlerConfigurationBuilder.build ();
          return crawlerConfiguration;
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }
}
