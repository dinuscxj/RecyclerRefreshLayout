package app.dinus.com.example.simplerefresh;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import app.dinus.com.example.R;

public class SimpleRefreshActivity extends AppCompatActivity {

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, SimpleRefreshActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, SimpleItemRecyclerFragment.newInstance())
                .commit();
    }
}
