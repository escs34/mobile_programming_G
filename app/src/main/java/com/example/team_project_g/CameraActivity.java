package com.example.team_project_g;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CameraActivity extends AppCompatActivity {
    /*
    기존의 main activity, 카메라 기능을 구현한 Activity

     */

    private static final int REQUEST_IMAGE_CAPTURE = 672;
    private String imageFilePath;
    private Uri photoUri;

    int camera_checker;
    int edit_checker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*단순 초기값 설정*/
        super.onCreate(savedInstanceState);

        TedPermission.with(getApplicationContext())
                .setPermissionListener(permissionListener)
                .setRationaleMessage("카메라 권한이 필요합니다.")
                .setDeniedMessage("거부하셨습니다.")
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .check();

        camera_checker = 0;
        edit_checker = 0;

    }

    public File createImageFile() throws IOException {
        /*임시 파일 생성함수*/
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "Test_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        imageFilePath = image.getAbsolutePath();
        return image;

    }



    @Override
    protected  void onStart(){
        /*검색 완료 후에 되돌아가면 또 카메라를 띄우는 부분이 돌아가도록 카메라 수행 및 second activity로 이동하는 코드를 onCreate가 아닌 onStart에 작성.

          카메라를 동작시키는 코드 자체가 activity를 잠시 멈추고 카메라 어플로 이동후 되돌아 오기 때문에 카메라 종료시에 activity가 다시 onStart
          상태에 들어서게 됨. 따라서 카메라를 찍을 때와 검색을 위한 activity로 넘어가는 부분을 if로 나눠서 작성

          camera_checker와 edit_checker로 상태 구분(semaphore처럼 검색부분 수행시 카메라 lock을 풀어줌)

         */
        super.onStart();

        if (camera_checker == 0){
            camera_checker = 1;


            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//카메라 사용 intent 생성
            if (intent.resolveActivity(getPackageManager()) != null){
                File photofile = null;
                try {
                    photofile = createImageFile();//임시 폴더에 파일 생성 <-여기에 데이터 들어갈 예정
                } catch (IOException e){

                }

                if (photofile != null){
                    photoUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName(), photofile);
                    edit_checker = 1;
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);//카메라의 Activity를 실행. 결과는 위에 생성한 임시 파일에 저장됨
                }
            }

        }
        else if(edit_checker == 1){
            camera_checker = 0;
            edit_checker = 0;
            Intent intent = new Intent(getApplicationContext(),
                    SecondActivity.class);
            intent.putExtra("uri",photoUri.toString());
            startActivity(intent);//검색을 위한 activity로
        }
    }


    PermissionListener permissionListener = new PermissionListener() {
        /*permission check를 위한 함수*/
        @Override
        public void onPermissionGranted() {
            Toast.makeText(getApplicationContext(), "권한이 허용됨", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            Toast.makeText(getApplicationContext(), "권한이 거부됨", Toast.LENGTH_SHORT).show();
        }
    };
}

