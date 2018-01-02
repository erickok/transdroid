package org.transdroid.daemon.Deluge;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Deluge RPC Request wrapper
 */
class Request {
  private static AtomicInteger requestIdCounter = new AtomicInteger();

  private final int id;
  private final String method;
  private final Object[] args;

  public Request(String method, Object... args) {
    id = requestIdCounter.getAndIncrement();
    this.method = method;
    this.args = args;
  }

  public Object toObject() {
    return new Object[] {id, method, args, new HashMap<>()};
  }

  public int getId() {
    return id;
  }

  public String getMethod() {
    return method;
  }

  public Object[] getArgs() {
    return args;
  }
}
