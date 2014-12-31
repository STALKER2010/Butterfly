package io.github.stalker2010.butterfly.tests;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import io.github.stalker2010.butterfly.Butterfly;
import io.github.stalker2010.butterfly.Callback;
import io.github.stalker2010.butterfly.TaskResult;

public class LogFragment extends Fragment implements View.OnClickListener {

    public LogFragment() {
    }

    TextView log;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        log = (TextView) rootView.findViewById(R.id.log);
        rootView.findViewById(R.id.start).setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            if (v instanceof Button) {
                final Button b = (Button) v;
                if (b.getId() == R.id.start) {
                    b.setEnabled(false);
                    start();
                }
            }
        }
    }

    int threadCount = 0;
    long startTime = 0;
    long sumOverhead = 0;

    public void preCallback() {
    }

    public void postCallback(final TaskResult res) {
        log.append("Task finished\n");
        threadCount--;
        {
            long o = res.executionTime - 1000;
            if (threadCount < 15) {
                o -= 1000;
            }
            if (threadCount < 10) {
                o -= 1000;
            }
            if (threadCount < 5) {
                o -= 1000;
            }
            System.out.println(o);
            sumOverhead += o;
        }
        if (threadCount < 1) {
            long diff = System.currentTimeMillis() - startTime;
            log.append("Time = " + diff + "ms. 5 threads. 20 tasks. At best each task takes 1000ms.\n");
            log.append("Total overhead is " + sumOverhead + "ms.\n");
            long overhead = sumOverhead / 20;
            log.append("It's about " + overhead + "ms for 1 task.\n");
        } else {
            log.append("Tasks left: " + threadCount + "\n");
        }
    }

    public void errorCallback(final Throwable e) {
        log.append(e.toString() + "\n");
    }

    public void progressCallback(final Bundle b) {
    }

    public void start() {
        log.append("Tests started\n");
        for (threadCount = 1; threadCount <= 20; threadCount++) {
            Butterfly.get()
                    .create("Test-" + threadCount, TestTask.class)
                    .pre(new Callback(this, "preCallback"))
                    .post(new Callback(this, "postCallback"))
                    .error(new Callback(this, "errorCallback"))
                    .progress(new Callback(this, "progressCallback"))
                    .run();
        }
        {
            // Workaround. I dunno why it can't work without it.
            threadCount--;
        }
        startTime = System.currentTimeMillis();
    }
}
