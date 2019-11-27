package com.example.team_project_g;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


//cropping api 출처 : https://github.com/ArthurHub/Android-Image-Cropper
public class SecondActivity  extends AppCompatActivity {
    //찍은 사진을 cropping 하고 http 통신으로 image를 hosting한 후, 구글 이미지 검색을 수행

    String response_url;
    String url_address;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second);
        setTitle("세컨드 액티비티");

        Intent intent = getIntent();

        String uri_str = intent.getStringExtra("uri");
        Uri imageUri = Uri.parse(uri_str);
        //CropImage.activity()
        //        .setGuidelines(CropImageView.Guidelines.ON)
        //        .start(this);

        // start cropping activity for pre-acquired image saved on the device
        CropImage.activity(imageUri)
                .start(this);

        Button btnEdit = (Button) findViewById(R.id.search);
        btnEdit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, url_address + response_url);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();

                JSONObject returned_response = null;
                File cropped_image_file = new File(resultUri.getPath());
                try {
                    returned_response = HttpMultiPart(cropped_image_file);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                //CropImageView cropImageView =  findViewById(R.id.cropImageView);
                ImageView crop_img_view = findViewById(R.id.crop_image_view); // <- way more better

                //cropImageView.setImageUriAsync(resultUri);

                crop_img_view.setImageURI(resultUri);

                try {
                    String response_data = returned_response.getString("data");
                    JSONObject response_data_json = new JSONObject(response_data);

                    response_url =  response_data_json.getString("url");

                    url_address = "https://www.google.com/searchbyimage?site=search&sa=X&image_url=";

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    //출처 : https://kwon8999.tistory.com/entry/HttpURLConnection-Multipart-%ED%8C%8C%EC%9D%BC-%EC%97%85%EB%A1%9C%EB%93%9C
    private JSONObject HttpMultiPart(final File file) throws ExecutionException, InterruptedException {

        JSONObject my_response = new AsyncTask<Void, Void, JSONObject>(){

            @Override
            protected JSONObject doInBackground(Void... voids) {

                String boundary = "^-----^";
                String LINE_FEED = "\r\n";
                String charset = "UTF-8";
                OutputStream outputStream;
                PrintWriter writer;

                JSONObject result = null;
                try{

                    String image_host_url = "https://api.imgbb.com/1/upload";
                    String api_key = "?key=3f4427ecf80247d9b7f57f130a0af6f7";//"?key=000fe55327b5c080c099f62956aee204";

                    URL url = new URL(image_host_url+api_key);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    connection.setRequestProperty("Content-Type", "multipart/form-data;charset=utf-8;boundary=" + boundary);
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);
                    connection.setConnectTimeout(15000);

                    outputStream = connection.getOutputStream();
                    writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);

                    writer.append("--" + boundary).append(LINE_FEED);
                    writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"" + file.getName() + "\"").append(LINE_FEED);
                    writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append(LINE_FEED);
                    writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
                    writer.append(LINE_FEED);
                    writer.flush();

                    FileInputStream inputStream = new FileInputStream(file);
                    byte[] buffer = new byte[(int)file.length()];
                    int bytesRead = -1;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                    inputStream.close();
                    writer.append(LINE_FEED);
                    writer.flush();

                    writer.append("--" + boundary + "--").append(LINE_FEED);
                    writer.close();

                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        try {
                            result = new JSONObject(response.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        result = new JSONObject(response.toString());
                    }
                } catch (ConnectException e) {
                    //Log.e(TAG, "ConnectException");
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                super.onPostExecute(jsonObject);
            }

        }.execute().get();
        return my_response;
    }
}
