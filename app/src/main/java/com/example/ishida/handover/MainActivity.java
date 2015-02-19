package com.example.ishida.handover;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Map;


public class MainActivity extends ActionBarActivity implements HandOverCallback {
    private static final String TAG = "HandOver Demo";
    private static final String EDIT_TEXT = "edit_text";
    private static final String SWITCH = "switch";

    private EditText editText;
    private Switch sw;

    private HandOver ho;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText)findViewById(R.id.editText);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "aId=" + actionId + ", " + event);
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // call handover here
                    ho.activityChanged();
                }
                return false;
            }
        });
        sw = (Switch)findViewById(R.id.switch1);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // call handover here
                ho.activityChanged();
            }
        });

        ho = HandOver.getHandOver(this);
        ho.registerCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ho.unbind();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void saveActivity(Map<String, Object> dictionary) {
        // save Objects to handover
        dictionary.put(EDIT_TEXT, editText.getText().toString());
        dictionary.put(SWITCH, sw.isChecked());
    }

    @Override
    public void restoreActivity(Map<String, Object> dictionary) {
        // restore handover'ed objects
        String text = (String) dictionary.get(EDIT_TEXT);
        boolean checked = (boolean) dictionary.get(SWITCH);
    }
}
