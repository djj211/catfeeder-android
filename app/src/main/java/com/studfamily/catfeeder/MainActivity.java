package com.studfamily.catfeeder;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.text.Editable;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;

public class MainActivity extends AppCompatActivity {

    private TextView feedTime = null;
    private Date lastFeedDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateTime();

        final Button button = findViewById(R.id.feed);
        final EditText feedTxt = findViewById(R.id.feedTime);

        feedTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (feedTxt.getText().toString().length() >= 2)
                {
                    InputMethodManager imm = (InputMethodManager) getSystemService(MainActivity.this.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(feedTxt.getWindowToken(), 0);
                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String feedTimer = feedTxt.getText().toString();

                if (feedTimer.equals("") || feedTimer == null)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("You Must Enter a Feed Interval!")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {}
                            });
                    AlertDialog alert = builder.create();
                    alert.show();

                }
                else if (lastFeedDate.after(new Date(System.currentTimeMillis() - 3600 * 4000)))
                {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    doFeed(feedTimer);
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Last feed less thatn 4 hours ago. Are you sure?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
                else {
                    doFeed(feedTimer);
                }
            }
        });
    }

    private void doFeed(String feedTimer) {
        Map<String, String> postData = new HashMap<>();
        postData.put("time", feedTimer);
        ExecuteFeederAsync task = new ExecuteFeederAsync(postData, MainActivity.this);
        task.execute("");
        updateTime();
    }

    private void updateTime() {
        feedTime = findViewById(R.id.lastFeedTime);
        ExecuteGetLastFeedAsync timeTask = new ExecuteGetLastFeedAsync();
        timeTask.execute("");

    }

    private class ExecuteGetLastFeedAsync extends AsyncTask<String,Void,String> {
        private String lastFeed = "";

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(String result) {

            if (lastFeed != null && !lastFeed.equals(""))
            {
                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy h:mm:ss aaa");

                try {
                    lastFeedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(lastFeed);

                    feedTime.setText("Last Feed on " + formatter.format(lastFeedDate));

                } catch (ParseException e) {
                    Log.e("Error Time", e.getLocalizedMessage());
                }
            }
            else
            {
                Log.e("WTF", "Date is null. Likely http error.");
            }

        }

        @Override
        protected String doInBackground(String... params) {
            try
            {
                URL url = new URL(params[0]);

                String username = "";
                String password = "";

                String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));  //Java 8

                // Create the urlConnection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();


                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                urlConnection.setRequestProperty("Content-Type", "application/json");

                urlConnection.setRequestMethod("POST");


                // OPTIONAL - Sets an authorization header
                urlConnection.setRequestProperty("Authorization", "Basic " + encoded);

                int statusCode = urlConnection.getResponseCode();
                Log.e("timer http status", Integer.toString(statusCode));

                if (statusCode ==  200)
                {
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                    String response = convertStreamToString(inputStream);
                    JSONObject obj = new JSONObject(response);

                    lastFeed = obj.getString("date");
                }

            } catch (Exception ex)
            {
                Log.e("Error Time", ex.getLocalizedMessage());
            }

            return null;
        }

        private String convertStreamToString(InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

}
