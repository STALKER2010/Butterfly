package io.github.stalker2010.butterfly.tests;

import android.os.Bundle;

import java.util.concurrent.TimeUnit;

import io.github.stalker2010.butterfly.ButterflyTask;
import io.github.stalker2010.butterfly.TaskResult;

/**
 * @author STALKER_2010
 */
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
            b.putString("stage", "before sleep");
            b.putInt("id", getId());
            progress(b);
        }
        // Do something, for example sleep.
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            // Fail. :(
            e.printStackTrace();
            return fail();
        }
        // Success.
        return success();
    }
}
