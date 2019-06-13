package com.studfamily.catfeeder;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class ExecuteFeederAsync extends AsyncTask<String,Void,String> {

    // This is the JSON body of the post
    private JSONObject postData;
    private Context context = null;
    private AlertDialog feedAlert;
    private Boolean success = true;
    private String errorMsg = "";

    // This is a constructor that allows you to pass in the JSON body
    public ExecuteFeederAsync(Map<String, String> postData, Context context) {
        if (postData != null) {
            this.postData = new JSONObject(postData);
            this.context = context;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Feeding");
        builder.setMessage("Feeding....")
                .setCancelable(false);
        feedAlert = builder.create();
        feedAlert.show();

    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        feedAlert.hide();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

        if (success)
        {
            alertDialog.setTitle("Success");
            alertDialog.setMessage("Feed Completed Successfully!");
        }
        else
        {
            alertDialog.setTitle("Error");
            alertDialog.setMessage(errorMsg);
        }

        alertDialog.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });

        AlertDialog complete = alertDialog.create();

        complete.show();
    }

    @Override
    protected String doInBackground(String... params) {
        try {
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

            // Send the post body
            if (this.postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();
            }

            int statusCode = urlConnection.getResponseCode();

            if (statusCode ==  200) {

                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                String response = convertStreamToString(inputStream);
                JSONObject obj = new JSONObject(response);

                if (obj.getString("result").equals("OK"))
                {
                    success = true;
                }
                else
                {
                    success = false;
                    errorMsg = "Error Returned by Server: " + obj.getString("result");
                }

            }
            else
            {
                success = false;
                errorMsg = "Server did not return status code 200. The following code was returned: " + Integer.toString(statusCode);
            }

        }
        catch (Exception ex)
        {
            Log.e("ASYNC FAIL", ex.getLocalizedMessage());
            success = false;
            errorMsg = "Error feeding: " + ex.getLocalizedMessage().toString();
        }

        return null;
    }

    private String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


}
