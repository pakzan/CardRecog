package com.example.tpz.cardrecog;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.util.ArrayList;

/**
 * Created by Dell on 09/11/2015.
 */
public class ServerRequests {

    private ProgressDialog progressDialog;
    private static final int CONNECTION_TIMEOUT = 1000 * 2;

    ServerRequests(Context context){
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("Please wait...");
    }

    void storeCardInfoInBackground(CardInfo cardInfo, String ipAddress, GetCallback cardInfoCallback){
        progressDialog.show();
        new storeCardInfoAsyncTask(cardInfo, ipAddress, cardInfoCallback).execute();
    }

    public class storeCardInfoAsyncTask extends AsyncTask<Void, Void, Void> {
        String ipAddress;
        CardInfo cardInfo;
        GetCallback callback;

        storeCardInfoAsyncTask(CardInfo cardInfo, String ipAddress, GetCallback callback){
            this.ipAddress = ipAddress;
            this.cardInfo = cardInfo;
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<NameValuePair> dataToSend = new ArrayList<>();
            dataToSend.add(new BasicNameValuePair("rank", cardInfo.rank));
            dataToSend.add(new BasicNameValuePair("suit", cardInfo.suit));

            HttpParams httpRequestParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpRequestParams, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpRequestParams, CONNECTION_TIMEOUT);

            HttpClient client = new DefaultHttpClient(httpRequestParams);
            HttpPost post = new HttpPost(ipAddress);

            try {
                post.setEntity(new UrlEncodedFormEntity(dataToSend));
                client.execute(post);
            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid){
            progressDialog.dismiss();
            callback.done();
            super.onPostExecute(aVoid);
        }
    }
}
