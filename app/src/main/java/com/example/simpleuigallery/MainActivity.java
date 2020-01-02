package com.example.simpleuigallery;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
    }

    private static final int REQUEST_PERMISSIONS = 1234;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PERMISSIONS_COUNT = 2;

    @SuppressLint("NewApi")
    private boolean arePermissionsDenied(){
        for(int i=0 ; i<PERMISSIONS_COUNT ; i++){
            if(checkSelfPermission(PERMISSIONS[i])!= PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions,final int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode == REQUEST_PERMISSIONS&&grantResults.length > 0){
            if(arePermissionsDenied()){
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else{
                onResume();
            }
        }
    }

    private List<String> filesList;

    private int selectedFileIndex;

    private boolean isGalleryInitialized;

    private GalleryAdapter galleryAdapter;

    private boolean isLongClick = false;

    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

        if(!isGalleryInitialized){
            filesList = new ArrayList<>();
            addImagesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)));
            addImagesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
            addImagesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)));

            final ListView listView = findViewById(R.id.listView);
            galleryAdapter = new GalleryAdapter();

            galleryAdapter.setData(filesList);
            listView.setAdapter(galleryAdapter);

            final TextView imageName = findViewById(R.id.imageName);
            final View topBar = findViewById(R.id.topBar);
            final View bottomBar = findViewById(R.id.bottomBar);

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){

                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if(!isLongClick){
                        isLongClick=true;
                        selectedFileIndex=position;
                        imageName.setVisibility(View.VISIBLE);
                        imageName.setText(filesList.get(position).substring(filesList.get(position).lastIndexOf('/')+1));
                        topBar.setVisibility(View.VISIBLE);
                        bottomBar.setVisibility(View.VISIBLE);
                    }else{
                        isLongClick=false;
                        imageName.setVisibility(View.GONE);
                        topBar.setVisibility(View.GONE);
                        bottomBar.setVisibility(View.GONE);
                    }
                    return false;
                }
            });

            final Button deleteButton = findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete");
                    deleteDialog.setMessage("Do you really want to delete the file?");
                    deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new File(filesList.get(selectedFileIndex)).delete();
                            filesList.remove(selectedFileIndex);
                            galleryAdapter.setData(filesList);
                            String fileName = filesList.get(selectedFileIndex);
                            Log.d("testing delete button:",fileName);
                        }
                    });
                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    deleteDialog.show();
                }
            });

            final Button renameButton = findViewById(R.id.renameButton);
            renameButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    final AlertDialog.Builder renameDialog =
                            new AlertDialog.Builder(MainActivity.this);
                    renameDialog.setTitle("Rename To:");
                    final EditText input = new EditText(MainActivity.this);
                    final String renamePath = filesList.get(selectedFileIndex);
                    input.setText(renamePath.substring(renamePath.lastIndexOf('/')));
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    renameDialog.setView(input);
                    renameDialog.setPositiveButton("Rename",new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which){
                            String s = new File(renamePath).getParent()+"/"+input.getText();
                            File newFile = new File(s);
                            new File(renamePath).renameTo(newFile);
                            filesList.remove(selectedFileIndex);
                            filesList.add(selectedFileIndex,s);
                            galleryAdapter.setData(filesList);
                            imageName.setText(input.getText());
                        }
                    });
                    renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    renameDialog.show();
                }
            });

            final Button shareButton = findViewById(R.id.shareButton);
            shareButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    final String filePath = filesList.get(selectedFileIndex);
                    final Uri imageUri = Uri.parse("file://"+filePath);
                    final Intent intent = new Intent(Intent.ACTION_SEND);
                    if(filePath.endsWith(".jpg")||filePath.endsWith(".png")){
                        intent.setType("image/png");
                    }else{
                        intent.setType("image/jpeg");
                    }
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.putExtra(Intent.EXTRA_STREAM,imageUri);
                    startActivity(Intent.createChooser(intent,"Share Image To:"));
                }
            });

            final Button infoButton = findViewById(R.id.showInfo);
            infoButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    final AlertDialog.Builder showInfoDialog = new AlertDialog.Builder(MainActivity.this);
                    showInfoDialog.setTitle("Image Info");
                    final File imageFile = new File(filesList.get(selectedFileIndex));
                    final Date lastModDate = new Date(imageFile.lastModified());
                    final long fileLength = imageFile.length()/1024;
                    String fileLengthString;
                    if(fileLength>1024){
                        fileLengthString = String.valueOf(fileLength/1024)+" MB";
                    }else{
                        fileLengthString = String.valueOf(fileLength)+" KB";
                    }

                    String info = "Location:"+filesList.get(selectedFileIndex)+"\n"
                            +"Last Modified:"+lastModDate.toString()+"\n"
                            +"Image Size:"+fileLengthString
                            +"Image Resolution:"+getImageResolution(filesList.get(selectedFileIndex));

                    showInfoDialog.setMessage(info);
                    showInfoDialog.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    showInfoDialog.show();
                }
            });

            final Button rotateLeftButton = findViewById(R.id.rotateLeft);
            rotateLeftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rotateImage(-90);
                }
            });

            final Button rotateRightButton = findViewById(R.id.rotateRight);
            rotateRightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rotateImage(90);
                }
            });

            isGalleryInitialized=true;
        }
    }

    private void addImagesFrom(String dirPath){
        final File imagesDir = new File(dirPath);
        final File[] files = imagesDir.listFiles();

        for(File file : files){
            final String path = file.getAbsolutePath();
            if(path.endsWith(".jpg")||path.endsWith(".png")||path.endsWith(".jpeg")||path.endsWith(".gif")){
                filesList.add(path);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void rotateImage(int rotation){
        //final BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
        final String filePath = filesList.get(selectedFileIndex);
        final Uri imageUri = Uri.parse("file://"+filePath);
        InputStream input =null;
        try{
            input = getContentResolver().openInputStream(imageUri);
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(input,null,null);
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        final Matrix matrix = new Matrix();
        matrix.setRotate(rotation);
        bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        final File outFile = new File(filePath);
        try(FileOutputStream out = new FileOutputStream(filePath)){
            if(filePath.endsWith("png")){
                bitmap.compress(Bitmap.CompressFormat.PNG,85,out);
            }else{
                bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Uri uri = Uri.parse("file://"+outFile.getAbsolutePath());
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE));
        galleryAdapter.notifyDataSetChanged();

    }

    private String getImageResolution(String imagePath){
        BitmapFactory.Options bOptions = new BitmapFactory.Options();
        bOptions.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(imagePath,bOptions);
        final int width = bOptions.outWidth;
        final int height = bOptions.outHeight;

        return String.valueOf(width+" X "+height);
    }

    final class GalleryAdapter extends BaseAdapter{

        private List<String> data = new ArrayList<>();

        void setData(List<String> data){
            if(this.data.size()>0){
                this.data.clear();
            }
            this.data.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public int getCount(){
            return data.size();
        }

        @Override
        public Object getItem(int position){
            return null;
        }

        @Override
        public long getItemId(int position){
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            ImageView imageView;
            if(convertView ==null){
                imageView = (ImageView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item,
                        parent,false);
            }else{
                imageView = (ImageView) convertView;
            }
            imageView.setImageBitmap(BitmapFactory.decodeFile(data.get(position)));
            return imageView;
        }
    }
}
