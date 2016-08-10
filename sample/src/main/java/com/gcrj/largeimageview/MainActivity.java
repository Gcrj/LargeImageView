package com.gcrj.largeimageview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.gcrj.largeimageviewlibrary.LargeImageView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LargeImageView liv = (LargeImageView) findViewById(R.id.liv);
        try {
            liv.setImage(getAssets().open("map.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
