package deluge.impl.net;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResponseExecutor
{

    ExecutorService mExecutor;

    public ResponseExecutor()
    {
        this.mExecutor = Executors.newFixedThreadPool(20);
    }

    public void execute(Runnable task)
    {
        this.mExecutor.execute(task);
    }

    public void shutdown()
    {
        if (this.mExecutor != null)
        {
            this.mExecutor.shutdown();
            while (!this.mExecutor.isTerminated())
            {
            }
            System.out.println("Finished all threads");
        }
    }
}
