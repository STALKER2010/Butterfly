package io.github.stalker2010.butterfly.tests;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import io.github.stalker2010.butterfly.Butterfly;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Butterfly.get().context(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new LogFragment())
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Butterfly.get().onDestroyActivity();
    }
}