package net.i2p.android.router.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import net.i2p.android.router.R;

public class MainActivity extends I2PActivityBase {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button news = (Button) findViewById(R.id.news_button);
        if (news == null) {
            System.err.println("No button resource!");
            return;
        }
        news.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), NewsActivity.class);
                startActivityForResult(intent, 0);
            }
        });
    }
}
