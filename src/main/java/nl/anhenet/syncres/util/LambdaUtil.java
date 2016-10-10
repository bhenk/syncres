package nl.anhenet.syncres.util;

/**
 * Created on 2016-10-05 16:18.
 */
public class LambdaUtil {

  @FunctionalInterface
  public interface Function_WithExceptions<T, R, E extends Exception> {
    R apply(T val) throws E;
  }
}
