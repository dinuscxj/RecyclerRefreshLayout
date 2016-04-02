package app.dinus.com.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import app.dinus.com.example.simplerefresh.SimpleRefreshActivity;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private Button mSimpleRefreshView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSimpleRefreshView = (Button) findViewById(R.id.simple_refresh);

        mSimpleRefreshView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.simple_refresh:
                SimpleRefreshActivity.startActivity(this);
                break;
        }
    }
}
