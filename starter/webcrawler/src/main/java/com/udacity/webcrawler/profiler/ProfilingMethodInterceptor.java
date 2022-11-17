package com.udacity.webcrawler.profiler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  public static final Predicate<Method> hasProfiledAnnotation = method -> method.getAnnotation(Profiled.class) !=null;
  private final Clock clock;
  private final Object delegate;
  private final ProfilingState state;

  // DONE: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock,Object delegate,ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate= Objects.requireNonNull(delegate);
    this.state = Objects.requireNonNull(state);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // DONE: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
    Object returnResult = null;
    boolean isProfiled = hasProfiledAnnotation.test(method);
    if(isProfiled){
      Instant start = clock.instant();
      try {
        returnResult = method.invoke(this.delegate,args);
      }  catch (InvocationTargetException e) {
        throw e.getTargetException();
      }finally {
        Instant end = clock.instant();
        state.record(delegate.getClass(), method,  Duration.between(start, end) );
      }
    }else {
      try {
        returnResult = method.invoke(this.delegate,args);
      }  catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
    }

    return returnResult;
  }
}
