package deluge.api;

public class DelugeException extends Exception
{
    private static final long serialVersionUID = 1L;

    final public String       exceptionType;
    final public String       exceptionMsg;
    final public String       traceback;

    public DelugeException(String type, String msg, String trace)
    {
        this.exceptionType = type;
        this.exceptionMsg = msg;
        this.traceback = trace;
    }

    @Override
    public void printStackTrace()
    {
        System.err.println(toString());
        System.err.println(this.traceback);
    }

    @Override
    public String toString()
    {
        return DelugeException.class.getCanonicalName() + " " + this.exceptionType + " (" + this.exceptionMsg + ")";
    }
}
