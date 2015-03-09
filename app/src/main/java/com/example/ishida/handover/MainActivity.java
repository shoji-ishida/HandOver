package com.example.ishida.handover;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
        Log.d(TAG, "onCreate");
        Log.d(TAG, Thread.currentThread().toString());

        editText = (EditText)findViewById(R.id.editText);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "aId=" + actionId + ", " + event);
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                    // call handover here
                    ho.activityChanged();
                }
                return false;
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            int currentLength = 0;
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                currentLength = s.toString().length();
            }

            @Override
            public void afterTextChanged(Editable s) {
                //Log.d(TAG, "after:" + s.toString());
                if (s.toString().length() < currentLength) {
                    return;
                }
                boolean unfixed = false;
                Object[] spanned = s.getSpans(0, s.length(), Object.class);
                if (spanned != null) {
                    for (Object obj : spanned) {
                        if (obj instanceof android.text.style.UnderlineSpan) {
                            unfixed = true;
                        }
                    }
                }
                if (!unfixed) {
                    Log.d(TAG, "Confirmed");
                    ho.activityChanged();
                }
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


        // for the testing purpose, we start a service here
        Intent serviceIntent = new Intent(this, HandOverService.class);
        this.startService(serviceIntent);

        ho = HandOver.getHandOver(this);
        ho.registerCallback(this);

        String action = getIntent().getAction();
        if (action.equals("com.example.ishida.handover.RECOVER")) {
            ho.restore();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        ho.unbind();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: " + getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        ho.bind();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
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
        Log.d(TAG, "text = " + editText.getText().toString());
        dictionary.put(EDIT_TEXT, editText.getText().toString());
        Log.d(TAG, "switch = " + sw.isChecked());
        dictionary.put(SWITCH, sw.isChecked());

        //dictionary.put("LONG_MAX", Long.MAX_VALUE);
        //dictionary.put("LONG_MIN", Long.MIN_VALUE);

        //dictionary.put("DOUBLE_MAX", Double.MAX_VALUE);
        //dictionary.put("DOUBLE_MIN", Double.MIN_VALUE);

    }

    @Override
    public void restoreActivity(Map<String, Object> dictionary) {
        // restore handover'ed objects
        final String text = (String) dictionary.get(EDIT_TEXT);
        final boolean checked = (boolean) dictionary.get(SWITCH);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.setText(text, TextView.BufferType.NORMAL);
                sw.setChecked(checked);
            }
        });
    }
}
