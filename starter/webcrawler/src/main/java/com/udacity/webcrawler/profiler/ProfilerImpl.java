package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) throws IllegalArgumentException{
    Objects.requireNonNull(klass);
    // DONE: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.
    if(Arrays.stream(klass.getDeclaredMethods())
            .noneMatch(ProfilingMethodInterceptor.hasProfiledAnnotation::test)){
      throw new IllegalArgumentException(klass.getName() + "doesn't have @Profiled annotated methods.");
    }
    InvocationHandler invocationHandler = new ProfilingMethodInterceptor(clock,delegate,state);
    T dynamicProxy = (T) Proxy.newProxyInstance( this.getClass().getClassLoader(), new Class[]{klass},invocationHandler);
    return dynamicProxy;
  }

  @Override
  public void writeData(Path path) {
    // DONE: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
    Writer writer = null;
    try {
      writer = Files.newBufferedWriter(path);
      writeData(writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }finally {
      if(writer != null){
        try {
          writer.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
