package net.i2p.android.router.addressbook;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.i2p.android.router.R;
import net.i2p.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AddressbookSettingsActivity extends ActionBarActivity {

    private EditText text_content_subscriptions;
    private Button btn_save_subscriptions;
    private String filename = "/addressbook/subscriptions.txt";
    private File i2pDir;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addressbook_settings);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        text_content_subscriptions = (EditText) findViewById(R.id.subscriptions_content);
        btn_save_subscriptions = (Button) findViewById(R.id.button_save_subscriptions);
        init_actions();
        i2pDir = new File(getFilesDir(), filename);
        load();
    }

    private void init_actions() {
        btn_save_subscriptions.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Context context = getApplicationContext();
                CharSequence text;
                if (save()) {
                    text = "subscriptions.txt successfully saved!";
                } else {
                    text = "there was a problem saving subscriptions.txt! Try fix permissions or reinstall i2p.";
                }
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean load() {
        String res = FileUtil.readTextFile(i2pDir.getAbsolutePath(), -1, true);
        if (res.length() > 0) {
            text_content_subscriptions.setText(res);
            return true;
        }
        Context context = getApplicationContext();
        CharSequence text = "Sorry, could not load subscriptions.txt!";
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        return false;
    }

    private boolean save() {
        //
        String content = text_content_subscriptions.getText().toString();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(i2pDir);
            byte[] contentInBytes = content.getBytes();
            out.write(contentInBytes);
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
        	if (out != null) try {out.close(); } catch (IOException ioe) {}
        }
    }
}
