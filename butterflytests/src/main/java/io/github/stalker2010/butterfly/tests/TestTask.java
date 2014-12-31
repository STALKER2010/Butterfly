package io.github.stalker2010.butterfly.tests;

import android.os.Bundle;

import java.util.concurrent.TimeUnit;

import io.github.stalker2010.butterfly.ButterflyTask;
import io.github.stalker2010.butterfly.TaskResult;

/**
 * @author STALKER_2010
 */
public class TestTask extends ButterflyTask {
    @Override
    public TaskResult doInBackground() {
        if (toStop()) return stop();
        {
            final Bundle b = new Bundle();
            b.putString("stage", "before sleep");
            b.putInt("id", getId());
            progress(b);
        }
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return fail();
        }
        {
            final Bundle b = new Bundle();
            b.putString("stage", "after sleep");
            b.putInt("id", getId());
            progress(b);
        }
        return success();
    }
}
