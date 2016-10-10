package nl.anhenet.syncres.view;


import nl.anhenet.syncres.xml.RsItem;

import java.util.function.Function;

/**
 *
 */
public class Interpreter {

  private Function<RsItem<?>, String> itemNameInterpreter = Interpreters.locItemNameInterpreter;
  private Function<Throwable, String> errorInterpreter = Interpreters.classAndMessageErrorInterpreter;

  public Function<RsItem<?>, String> getItemNameInterpreter() {
    return itemNameInterpreter;
  }

  public Interpreter withItemNameInterpreter(Function<RsItem<?>, String> itemNameInterpreter) {
    this.itemNameInterpreter = itemNameInterpreter;
    return this;
  }

  public Function<Throwable, String> getErrorInterpreter() {
    return errorInterpreter;
  }

  public Interpreter withErrorInterpreter(Function<Throwable, String> errorInterpreter) {
    this.errorInterpreter = errorInterpreter;
    return this;
  }

  public Interpreter withStackTrace(boolean debug) {
    if (debug) {
      errorInterpreter = Interpreters.stacktraceErrorInterpreter;
    }
    return this;
  }
}
