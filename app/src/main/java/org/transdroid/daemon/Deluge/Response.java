package org.transdroid.daemon.Deluge;

import java.util.List;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;

/**
 * A Deluge RPC Response wrapper
 */
class Response {

  private static final int RESPONSE_TYPE_INDEX = 0;
  private static final int RESPONSE_ID_INDEX = 1;
  private static final int RESPONSE_RETURN_VALUE_INDEX = 2;

  private final int type;
  private final int id;
  private final Object returnValue;

  public Response(Object responseObject) throws DaemonException {
    if (!(responseObject instanceof List)) {
      throw new DaemonException(ExceptionType.UnexpectedResponse, responseObject.toString());
    }
    final List response = (List) responseObject;

    if (response.size() < RESPONSE_RETURN_VALUE_INDEX + 1) {
      throw new DaemonException(ExceptionType.UnexpectedResponse, responseObject.toString());
    }

    if (!(response.get(RESPONSE_TYPE_INDEX) instanceof Number)) {
      throw new DaemonException(ExceptionType.UnexpectedResponse, responseObject.toString());
    }
    type = ((Number) (response.get(RESPONSE_TYPE_INDEX))).intValue();

    if (!(response.get(RESPONSE_ID_INDEX) instanceof Number)) {
      throw new DaemonException(ExceptionType.UnexpectedResponse, responseObject.toString());
    }
    id = ((Number) (response.get(RESPONSE_ID_INDEX))).intValue();

    returnValue = response.get(RESPONSE_RETURN_VALUE_INDEX);
  }

  public int getType() {
    return type;
  }

  public int getId() {
    return id;
  }

  public Object getReturnValue() {
    return returnValue;
  }

}
