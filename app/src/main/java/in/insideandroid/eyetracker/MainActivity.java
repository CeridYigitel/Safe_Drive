package in.insideandroid.eyetracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

import static in.insideandroid.eyetracker.Condition.FACE_NOT_FOUND;
import static in.insideandroid.eyetracker.Condition.USER_EYES_CLOSED;


public class MainActivity extends AppCompatActivity {
    final long FIRST_ALARM_DURATION = 3000;
    long SECOND_ALARM_DURATION = 7000;
    Button Baslat,Durdur;
    SeekBar seekbar;
    ConstraintLayout background;
    TextView user_message;
    String PreviousState = "";
    boolean flag = false;
    CameraSource cameraSource;
    ImageView iv;
    boolean baslat =false;
    boolean firsttime=true;
    MediaPlayer mPlayer;
    //boolean start_counting = false;
    int previous_state = 2; //0 is open, 1 is closed, 2 is not found
    long when_eyes_closed;
    boolean is_initial_alarm_triggered = false;
    boolean is_secondary_alarm_triggered = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            Toast.makeText(this, "Permission not granted!\n Grant permission and restart app", Toast.LENGTH_SHORT).show();
        }else{
            init();
        }
    }

    private void init() {
        iv = (ImageView) findViewById(R.id.ad_image_view);
        iv.setImageResource(R.drawable.openeyes);
        background = findViewById(R.id.background);
        user_message = findViewById(R.id.user_text);
        Baslat = findViewById(R.id.Baslat);
        Durdur = findViewById(R.id.Durdur);

        //user_message.setHeight(400);
        //user_message.setWidth(800);
        flag = true;

        seekbar = (SeekBar) findViewById(R.id.seekBar2);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                SECOND_ALARM_DURATION = (progress + 5) * 1000;
            }
        });

        initCameraSource();
    }
    private void initCameraSource() {
        Baslat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baslat=true;
                if(firsttime){
                    if(mPlayer!=null)
                    {
                        mPlayer.release();
                        mPlayer=null;
                    }
                    mPlayer = MediaPlayer.create(MainActivity.this,
                            R.raw.welcome);
                    mPlayer.start();
                }
                if(PreviousState == FACE_NOT_FOUND.toString()) {
                    setBackgroundRed();
                    user_message.setText("User Not Found, Please Set Lighting");

                    mPlayer = MediaPlayer.create(MainActivity.this,
                            R.raw.facenotfound);
                    mPlayer.start();
                    mPlayer.setLooping(true);
                }

                firsttime=false;
            }
        });
        Durdur.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baslat=false;
                if(mPlayer != null) {
                    mPlayer.release();
                    mPlayer = null;
                }
                setBackgroundGrey();
                user_message.setText("Stopped");
            }
        });

        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        detector.setProcessor(new MultiProcessor.Builder(new FaceTrackerDaemon(MainActivity.this)).build());

        cameraSource = new CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(1024, 768)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraSource.start();
        }
        catch (IOException e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraSource != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                cameraSource.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraSource!=null) {
            cameraSource.stop();
        }

        setBackgroundGrey();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource!=null) {
            cameraSource.release();
        }
    }


    public void updateMainView(Condition condition) {
        if (baslat) {
            switch (condition) {
                case USER_EYES_OPEN:
                    when_eyes_closed = System.currentTimeMillis();
                    is_initial_alarm_triggered = is_secondary_alarm_triggered = false;
                    if((PreviousState == FACE_NOT_FOUND.toString() || PreviousState == USER_EYES_CLOSED.toString()) && mPlayer != null) {
                        mPlayer.release();
                        mPlayer = null;
                    }
                    if (condition.toString() == PreviousState)
                        break;
                    setBackgroundGreen();
                    iv.setImageResource(R.drawable.openeyes);
                    user_message.setText("Open Eyes Detected, Please Drive Safe");
                    PreviousState = condition.toString();
                    break;
                case USER_EYES_CLOSED:
                    if(PreviousState == FACE_NOT_FOUND.toString()) {
                        //mPlayer.stop();
                        mPlayer.release();
                        mPlayer = null;
                    }
                    if (condition.toString() == PreviousState) {
                        //System.out.println("when: " + when_eyes_closed);
                        //System.out.println("curr: " + System.currentTimeMillis());
                        //System.out.println("diff: " + (System.currentTimeMillis() - when_eyes_closed));

                        if (System.currentTimeMillis() - when_eyes_closed > FIRST_ALARM_DURATION ) {
                            if(!is_initial_alarm_triggered) {
                                is_initial_alarm_triggered = true;
                                if (mPlayer != null) {
                                    mPlayer.release();
                                    mPlayer = null;
                                }
                                mPlayer = MediaPlayer.create(MainActivity.this,
                                        R.raw.threesecpass);
                                mPlayer.start();
                            }
                            if(System.currentTimeMillis() - when_eyes_closed > SECOND_ALARM_DURATION && !is_secondary_alarm_triggered){
                                is_secondary_alarm_triggered = true;
                                if (mPlayer != null) {
                                    mPlayer.release();
                                    mPlayer = null;
                                }
                                mPlayer = MediaPlayer.create(MainActivity.this,
                                        R.raw.alertsound);
                                mPlayer.start();
                            }
                        }

                        break;
                    }

                    when_eyes_closed = System.currentTimeMillis();
                    setBackgroundOrange();
                    user_message.setText("Close Eyes Detected, Please Take a Break If You Need");
                    iv.setImageResource(R.drawable.closedeye);
                    PreviousState = condition.toString();
                    /*if(mPlayer!=null)
                    {
                        mPlayer.release();
                        mPlayer=null;
                    }
                    mPlayer = MediaPlayer.create(MainActivity.this,
                            R.raw.threesecpass);
                    mPlayer.start();*/
                    break;
                case FACE_NOT_FOUND:


                    setBackgroundRed();
                    user_message.setText("User Not Found, Please Adjust Camera Direction");
                    PreviousState = condition.toString();
                    is_initial_alarm_triggered = is_secondary_alarm_triggered = false;
                    if(mPlayer!=null)
                    {
                        mPlayer.stop();
                        mPlayer.release();
                        mPlayer=null;
                    }
                    mPlayer = MediaPlayer.create(MainActivity.this,
                            R.raw.facenotfound);
                    mPlayer.start();
                    mPlayer.setLooping(true);
                    break;
                    /*if(mPlayer!=null)
                    {
                        mPlayer.release();
                        mPlayer=null;
                    }
                    break;*/
                default:
                    setBackgroundGrey();
            }
        }
        if(!baslat){
            if(mPlayer!=null)
            {
                mPlayer.release();
                mPlayer=null;
            }
            user_message.setText("Stopped");
            PreviousState=null;
        }
    }

    private void setBackgroundGrey() {
        if(background != null)
            background.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
    }


    private void setBackgroundGreen() {
        if(background != null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    background.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            });
        }
    }


    private void setBackgroundOrange() {
        if(background != null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    background.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
                }
            });
        }
    }


    private void setBackgroundRed() {
        if(background != null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    background.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            });
        }
    }
}
