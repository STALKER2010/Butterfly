Butterfly
=========

Async task engine.

# How to use Butterfly?
### Activity
In onCreate add ```Butterfly.get().context(this)``` BEFORE ```super.onCreate(sav)```.

In onDestroy add ```Butterfly.get().onDestroyActivity()``` AFTER ```super.onDestroy()```.
### Task
Example:
```java
import android.os.Bundle;
import java.util.concurrent.TimeUnit;
import io.github.stalker2010.butterfly.ButterflyTask;
import io.github.stalker2010.butterfly.TaskResult;

// We should extend ButterflyTask
public class TestTask extends ButterflyTask {
    // And implement doInBackground method, which returns TaskResult
    @Override
    public TaskResult doInBackground() {
        // Do it regularily - we should be able to stop as quick as possible.
        if (toStop()) return stop();
        // Set progress
        {
            final Bundle b = new Bundle();
            b.putInt("id", getId());
            progress(b);
        }
        // Do something, for example sleep.
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // Fail. :(
            return fail();
        }
        // Why not to set progress again?
        {
            final Bundle b = new Bundle();
            b.putInt("id", getId());
            progress(b);
        }
        // Success.
        return success();
    }
}
```
# To contributors
Pull requests are welcome!
