package net.i2p.android.router.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


import net.i2p.android.router.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddressbookSettingsActivity extends Activity {

	protected EditText text_content_subscriptions;
    protected Button btn_save_subscriptions;
    private String filename = "/addressbook/subscriptions.txt";
    private String i2pDir;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addressbook_settings);
        text_content_subscriptions = (EditText) findViewById(R.id.subscriptions_content);
        btn_save_subscriptions = (Button) findViewById(R.id.button_save_subscriptions);
        init_actions();
        i2pDir = getFilesDir().getAbsolutePath();
        load();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_addressbook_settings, menu);
        return true;
    }
    
    private void init_actions() {
    	btn_save_subscriptions.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	Context context = getApplicationContext();
            	CharSequence text = "";
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
    	String res = null;
    	FileInputStream in;
		try {
			in = new FileInputStream(new File(i2pDir+filename));
			if (in != null) {
	    		InputStreamReader input = new InputStreamReader(in);
	    		BufferedReader buffreader = new BufferedReader(input);
	    		res="";
	    		String line = null;
	    		while (( line = buffreader.readLine()) != null) {
	    			res += line+"\n";
	    		}
	    		in.close();
	    		text_content_subscriptions.setText(res);
	    		return true;
			}
		} catch (Exception e) {
			Log.e("I2P-AddressbookSettings", "Can't read subscriptions.txt");
			//TODO: Add error reporting support
			e.printStackTrace();
			return false;
		}
		return false;
    }
    
    private boolean save() {
    	//
    	String content = text_content_subscriptions.getText().toString();
    	try {
    		FileOutputStream out = new FileOutputStream(new File(i2pDir+filename));
			byte[] contentInBytes = content.getBytes();
    		out.write(contentInBytes);
			out.flush();
			out.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
    }
}
