package com.dinuscxj.example.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.dinuscxj.example.R;

public class OpenProjectRefreshActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, OpenProjectTabPagerFragment.newInstance())
                .commit();
    }
}
