package com.example.hustarmobileapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AddStoreActivity extends AppCompatActivity {
    private  static String IP_ADDRESS = "192.168.0.35";
    private final String TAG = "AddStoreActivity";
    private Spinner storeCategorySpinner;
    private TextView cateTextView, addressTextView;
    private EditText storeNameEditText, tableNumberEditText, storeNumberEdittText, storeDescriptEditText, storeAdEditText;

    private int seq;
    private int storeSeq;
    private int tableCount;
    private int tableSeq;

    private String tJsonString;
    private String mJsonString;
    private ArrayList<String> cateArrayList = new ArrayList<String>();
    private ArrayAdapter<String> arrayAdapter;

    private String absoultePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_store);
        Log.i(TAG, "onCraete---------------start");
        findViewById(R.id.storeImgaeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Log.i(TAG, "?????? : "+absoultePath);
                    }
                };
            }
        });
        init();
        Log.i(TAG, "onCreate---------------end");
    }


    private void init() {
        Log.i(TAG, "init---------------start");
        Intent intent = getIntent();
        seq = intent.getExtras().getInt("seq");
        //????????? ??????, ?????? ??????..

        storeNameEditText = findViewById(R.id.storeNameEditText);
        storeDescriptEditText = findViewById(R.id.storeDescriptEditText);
        tableNumberEditText = findViewById(R.id.tableNumberEditText);
        storeNumberEdittText = findViewById(R.id.storeNumberEditText);
        cateTextView = findViewById(R.id.cateTextView);
        storeAdEditText = findViewById(R.id.storeAdEditText);
        addressTextView = findViewById(R.id.addressTextView);
        storeCategorySpinner = findViewById(R.id.storeCategorySpinner);

        GetData task = new GetData();
        task.execute("http://"+IP_ADDRESS+"/SelectCate.php", "");

        arrayAdapter = new ArrayAdapter<>(AddStoreActivity.this, android.R.layout.simple_spinner_dropdown_item, cateArrayList);
        storeCategorySpinner.setAdapter(arrayAdapter);
        storeCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG,cateArrayList.get(i));
                cateTextView.setText(cateArrayList.get(i));
                //Toast.makeText(AddStoreActivity.this, cateArrayList.get(i), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.i(TAG,"onNothingSelected");
            }
        });
        Log.i(TAG, "init---------------end");

    }

    public void onClickAddStore(View v){
        String memberSeq = Integer.toString(seq);
        int cateSeq = cateArrayList.indexOf(cateTextView.getText().toString())+1;
        String categorySeq = Integer.toString(cateSeq);
        String storeName = storeNameEditText.getText().toString();
        String storeAddress = addressTextView.getText().toString()+ " " + storeAdEditText.getText().toString();
        String storeIntro = storeDescriptEditText.getText().toString();
        String storeNumber = storeNumberEdittText.getText().toString();
        String tableNumber = tableNumberEditText.getText().toString();
        tableCount = Integer.parseInt(tableNumber);
        if(storeAddress.length()==0 || storeName.length()==0 || storeIntro.length()==0 || storeNumber.length()==0 || tableNumber.length()==0){
            AlertDialog.Builder builder = new AlertDialog.Builder(AddStoreActivity.this);
            builder.setTitle("ADD STORE").setMessage("?????? ????????? ?????????.");
            builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            builder.show();
            return;
        }
        InsertData task = new InsertData();
        task.execute("http://"+IP_ADDRESS+"/insertstore.php",memberSeq,categorySeq,storeName,storeAddress,storeIntro,storeNumber, tableNumber);

    }

    public void goAddressWeb(View v){
        Intent intent = new Intent(AddStoreActivity.this, AddressWebViewActivity.class);
        startActivityForResult(intent, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == -1){
            switch (requestCode) {
                case 1000: {
                    String resultData = data.getExtras().getString("address");
                    addressTextView.setText(resultData);
                }
            }
        }

    }

    private class GetData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(AddStoreActivity.this, "????????? . . ", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();

            if(result == ""){
                Toast.makeText(AddStoreActivity.this, "?????? ??????", Toast.LENGTH_SHORT).show();
            }else{
                mJsonString = result;
                showResult();
                arrayAdapter.notifyDataSetChanged();

            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];
            String postParameters = "parameter=" + params[1];

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                bufferedReader.close();
                Log.i(TAG, "CHECK RETURN VALUE ::::::::::::::::::::: " + sb.toString().trim());
                return sb.toString().trim();
            } catch (Exception e) {
                Log.d(TAG, "GteData: Error ", e);
                errorString = e.toString();
                return null;
            }
        }

        private void showResult(){
            String TAG_JSON = "zari";
            String TAG_CATE_NAME = "categoryName";
            try {
                JSONObject jsonObject = new JSONObject(mJsonString);
                JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);
                for(int i =0; i<jsonArray.length(); i++){
                    JSONObject item = jsonArray.getJSONObject(i);
                    String cateItem = item.getString(TAG_CATE_NAME);
                    cateArrayList.add(cateItem);
                    Log.i(TAG, cateArrayList.get(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class InsertData extends AsyncTask<String, Void, String>{
        ProgressDialog progressDialog;
        String errorString;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(AddStoreActivity.this, "????????? . . ", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(TAG, "POST INSERT STORE response  - " + result);
            storeSeq = Integer.parseInt(result);
                /*
                GetTableSeq task = new GetTableSeq();
                Log.d(TAG, "????????? SEQ ??????????????? storeSeq ????????????  - " + storeSeq);
                task.execute("http://" + IP_ADDRESS + "/selecttableseq.php", Integer.toString(storeSeq));


                ?????? ?????? ?????? ????????? ????????? ?????? ?????? ?????? ?????????????????? ?????? ?????? ????????? ?????????
                ????????? ???????????? ?????? ??????????

                Intent intent = new Intent(AddStoreActivity.this, MasterPostActivity.class);
                intent.putExtra("memberSeq", seq);
                intent.putExtra("storeSeq", storeSeq);
                intent.putExtra("tableCount", tableCount);
                Log.i(TAG, "?????? TABELSEQ ???????????????" + tableSeq);
                intent.putExtra("tableSeq", tableSeq);

                startActivity(intent);
                finish();
                */
            AlertDialog.Builder builder = new AlertDialog.Builder(AddStoreActivity.this);
            builder.setTitle("ADD STORE").setMessage("??????????????? ?????????????????????. ?????? ????????? ??? ?????????.");
            builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    Intent intent = new Intent(AddStoreActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
            builder.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];
            String memberSeq = params[1]; //int
            String categorySeq = params[2]; //int
            String storeName = params[3];
            String storeAddress = params[4];
            String storeIntro = params[5];
            String storeNumber = params[6];
            String tableNumber = params[7];

            String postParameter = "memberSeq="+memberSeq+"&categorySeq="+categorySeq+"&storeName="+storeName+"&storeAddress="+storeAddress+"&storeIntro="+storeIntro+"&storeNumber="+storeNumber+"&tableNumber="+tableNumber;

            try{
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();;

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameter.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "POST response : "+responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK){
                    inputStream = httpURLConnection.getInputStream();
                }else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while ((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }
                bufferedReader.close();
                return  sb.toString();
            } catch (Exception e){
                Log.d(TAG, "InsertData : Error ", e);
                return new String("Error: " + e.getMessage());
            }
        }
    }

    private class GetTableSeq extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(AddStoreActivity.this, "????????? . . ", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();

            if(result == ""){
                Toast.makeText(AddStoreActivity.this, "?????? ??????", Toast.LENGTH_SHORT).show();
            }else{
                tJsonString = result;
                showTableResult();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];
            String postParameters = "storeSeq=" + params[1];

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                bufferedReader.close();
                Log.i(TAG, "CHECK RETURN VALUE ::::::::::::::::::::: " + sb.toString().trim());
                return sb.toString().trim();
            } catch (Exception e) {
                Log.d(TAG, "GteeeeeData: Error ", e);
                errorString = e.toString();
                return null;
            }
        }

        private void showTableResult(){
            String TAG_JSON = "zari";
            String TAG_TABLE_SEQ = "tableSeq";
            try {
                JSONObject jsonObject = new JSONObject(tJsonString);
                JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);
                JSONObject item = jsonArray.getJSONObject(0);

                tableSeq = item.getInt(TAG_TABLE_SEQ);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}