package com.twm.pt.demo.badminton;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.twm.pt.demo.badminton.Manager.PictureManager;
import com.twm.pt.demo.badminton.utility.L;
import com.twm.pt.demo.badminton.utility.PreferenceUtils;
import com.twm.pt.demo.badminton.utility.StorageDirectory;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Context mContext;

    private View app_bar_main;
    private TextView score1, score2;
    private TextView play1, play2, play3, play4;
    private ImageView userImageView;
    private TextView nav_userName, nav_userEmail, nav_gameName, nav_versionName;
    private TextView teamA, teamB;

    private Toolbar toolbar;
    private MenuItem action_sound, action_nosound;
    private MenuItem actionUp, actionDown;

    private long score1_num=0, score2_num=0;
    private Firebase myFirebaseRef;
    private String fireBaseBadmintonKey = "badminton";
    private String badmintonGameNameKey = "badmintonGameName";
    private String badmintonGameName = "Demo Game";
    private String score1Key = "score1";
    private String score2Key = "score2";
    private String lastUpdateKey = "lastUpdate";
    private String isUpkey = "isUp";
    private boolean isUp = true;
    private String lastUpdate = "A";
    private float scoreTextSize = 270f;
    private HistoryData mHistoryData = new HistoryData();

    private TextToSpeech tts;
    private String useTTSKey = "useTTS";
    private boolean useTTS = true;

    private Animation mAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!doDataBack()) {
                    Snackbar.make(view, "No Data Back!", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            }
        });
        //fab.setVisibility(View.GONE);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        init();
        initView();
        initFirebase();
        initUserAcc();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStop() {
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        action_sound= menu.findItem(R.id.action_sound);
        action_nosound= menu.findItem(R.id.action_nosound);
        actionUp = menu.findItem(R.id.action_up);
        actionDown = menu.findItem(R.id.action_down);
        displaySoundMenuItem(useTTS);
        displayCalMenuItem(isUp);
        return true;
    }
    private void displaySoundMenuItem(boolean useTTS) {
        this.useTTS = useTTS;
        action_sound.setVisible(useTTS);
        action_nosound.setVisible(!useTTS);
        PreferenceUtils.setValue(mContext, useTTSKey, useTTS);
    }
    private void displayCalMenuItem(boolean isUp) {
        this.isUp = isUp;
        actionUp.setVisible(isUp);
        actionDown.setVisible(!isUp);
        PreferenceUtils.setValue(mContext, isUpkey, isUp);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear) {
            clearPlayScore();
        } else if (id == R.id.action_sound) {
            displaySoundMenuItem(false);
        } else if (id == R.id.action_nosound) {
            displaySoundMenuItem(true);
        } else if (id == R.id.action_up) {
            displayCalMenuItem(false);
        } else if (id == R.id.action_down) {
            displayCalMenuItem(true);
        } else if (id == R.id.action_switch) {
            switchScore();
            displayScore();
        }
        return true;
        //return super.onOptionsItemSelected(item);
    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_newGame) {
            createNewGameAlertDialog();
        } else if (id == R.id.nav_joinGame) {
            joinGame();
        } else if (id == R.id.nav_share) {
//            Toast.makeText(mContext, "未完，待續", Toast.LENGTH_SHORT).show();
            shotScreenAndShare(app_bar_main);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private void init() {
        mContext = this;
        tts = new TextToSpeech(getApplicationContext(),mOnInitListener);
        badmintonGameName = PreferenceUtils.getValue(mContext, badmintonGameNameKey, "Demo Game");
        useTTS = PreferenceUtils.getValue(mContext, useTTSKey, true);
        isUp = PreferenceUtils.getValue(mContext, isUpkey, true);
    }

    private TextToSpeech.OnInitListener mOnInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if(status==TextToSpeech.SUCCESS){
                L.d("TTS ready!");
            }else{
                L.e("TTS error status=" + status);
            }
        }
    };

    private void initFirebase() {
        L.d("initFirebase");
        nav_gameName.setText(badmintonGameName);
        toolbar.setSubtitle(badmintonGameName);
        Firebase.setAndroidContext(this);
        myFirebaseRef = new Firebase("https://amber-fire-7015.firebaseIO.com/");

        myFirebaseRef.child(fireBaseBadmintonKey).child(badmintonGameName).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                //L.d("onDataChange: " + badmintonGameName + ", " + snapshot.getValue());
                score1_num = getPlayScoreFromObject(snapshot.child(score1Key).getValue());
                score2_num = getPlayScoreFromObject(snapshot.child(score2Key).getValue());
                if (snapshot.child(lastUpdateKey).getValue() != null) {
                    lastUpdate = (String) snapshot.child(lastUpdateKey).getValue();
                }
                if(mHistoryData.add(new DateInfo(score1_num, score2_num, lastUpdate))) {
                    if (score1_num == score2_num) {
                        if(score1_num==0) {
                            ttsSpeak(" 比賽開始 ");
                        } else if(score1_num>=20) {
                            ttsSpeak(score1_num + " 平,  丟士");
                        } else {
                            ttsSpeak(score1_num + " 平 ");
                        }
                    } else if(lastUpdate.equals("A")) {
                        ttsSpeak(score1_num + " 比 " + score2_num);
                    } else if (lastUpdate.equals("B")) {
                        ttsSpeak(score2_num + " 比 " + score1_num);
                    }
                    displayScore();
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
                L.d("onCancelled");
            }
        });
    }



    private void initView() {
        app_bar_main = getView(R.id.app_bar_main);

        play1 = getView(R.id.play1);
        play2 = getView(R.id.play2);
        play3 = getView(R.id.play3);
        play4 = getView(R.id.play4);

        score1 = getView(R.id.score1);
        score2 = getView(R.id.score2);
        score1.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Datacron Bold.ttf"));
        score2.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Datacron Bold.ttf"));
        scoreTextSize = score1.getTextSize();

        teamA = getView(R.id.teamA);
        teamB = getView(R.id.teamB);

        userImageView = getView(R.id.nav_userImageView);
        nav_userName = getView(R.id.nav_userName);
        nav_userEmail = getView(R.id.nav_userEmail);
        nav_gameName = getView(R.id.nav_gameName);
        nav_versionName = getView(R.id.nav_versionName);

        score1.setOnClickListener(scoreAOnClickListener);
        score2.setOnClickListener(scoreBOnClickListener);
        nav_versionName.setText("V" + getVersionName());

        mAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);
    }

    View.OnClickListener scoreAOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View paramView) {
            if (isUp) score1_num++;
            else score1_num--;
            lastUpdate = "A";
            displayScore();

            myFirebaseRef.child(fireBaseBadmintonKey).child(badmintonGameName).child(lastUpdateKey).setValue(lastUpdate);
            myFirebaseRef.child(fireBaseBadmintonKey).child(badmintonGameName).child(score1Key).setValue(score1_num);
        }
    };

    View.OnClickListener scoreBOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View paramView) {
            if (isUp) score2_num++;
            else score2_num--;
            lastUpdate = "B";
            displayScore();

            myFirebaseRef.child(fireBaseBadmintonKey).child(badmintonGameName).child(lastUpdateKey).setValue(lastUpdate);
            myFirebaseRef.child(fireBaseBadmintonKey).child(badmintonGameName).child(score2Key).setValue(score2_num);
        }
    };



    private long getPlayScoreFromObject(Object obj) {
        if (obj instanceof Long) {
            return (long) obj;
        }
        return 0;
    }




    private void clearPlayScore() {
        mHistoryData.clear();
        score1_num = 0;
        score2_num = 0;
        score1.setTextSize(TypedValue.COMPLEX_UNIT_PX, scoreTextSize);
        score2.setTextSize(TypedValue.COMPLEX_UNIT_PX, scoreTextSize);
        displayScore();
        myFirebaseRef.child(fireBaseBadmintonKey).child(badmintonGameName).child(score1Key).setValue(score1_num);
        myFirebaseRef.child(fireBaseBadmintonKey).child(badmintonGameName).child(score2Key).setValue(score2_num);
    }

    private void switchScore() {
        TextView tempScore;
        tempScore = score1;
        score1 = score2;
        score2 = tempScore;
        score1.setOnClickListener(scoreAOnClickListener);
        score2.setOnClickListener(scoreBOnClickListener);

        TextView tempPlay;
        tempPlay = play1;
        play1 = play4;
        play4 = tempPlay;
        tempPlay = play2;
        play2 = play3;
        play3 = tempPlay;

        tempPlay = teamA;
        teamA = teamB;
        teamB = tempPlay;

        teamA.setText(R.string.teamA);
        teamB.setText(R.string.teamB);
    }
    private void displayScore() {
        setPlayScore();
        setPlayerColor();
    }
    private void setPlayScore() {
        setPlayScoreTextView(score1, score1_num);
        setPlayScoreTextView(score2, score2_num);
    }
    private void setPlayScoreTextView(TextView v, long score) {
        v.setText("" + score);
        if(score>=20) {
            if(score==20) {
                v.startAnimation(mAnimation);
            }
            v.setTextColor(ContextCompat.getColor(mContext, R.color.red));
        } else {
            v.setTextColor(ContextCompat.getColor(mContext, R.color.blue));
        }
        while(v.getLineCount()>1) {
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX, v.getTextSize() - 20f);
            v.setText("" + score);
        }
    }

    private void setPlayerColor() {
        if (lastUpdate == null) {
        } else if (lastUpdate.equals("A")) {
            setPlayerAColor();
        } else if (lastUpdate.equals("B")) {
            setPlayerBColor();
        }
    }


    private void setPlayerAColor() {
        if (score1_num == 0) {
            play1.setBackgroundResource(R.drawable.border);
            play2.setBackgroundResource(R.drawable.border);
        } else if (score1_num % 2 == 0) {
            play1.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
            play2.setBackgroundResource(R.drawable.border);
        } else {
            play1.setBackgroundResource(R.drawable.border);
            play2.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
        }
        play3.setBackgroundResource(R.drawable.border);
        play4.setBackgroundResource(R.drawable.border);
    }

    private void setPlayerBColor() {
        if (score2_num == 0) {
            play3.setBackgroundResource(R.drawable.border);
            play4.setBackgroundResource(R.drawable.border);
        } else if (score2_num % 2 == 0) {
            play3.setBackgroundResource(R.drawable.border);
            play4.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
        } else {
            play3.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
            play4.setBackgroundResource(R.drawable.border);
        }
        play1.setBackgroundResource(R.drawable.border);
        play2.setBackgroundResource(R.drawable.border);
    }


    private void initUserAcc() {
        AccountManager accountManager = AccountManager.get(mContext);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        for(Account account : accounts){
            L.d("account=" + account.name + ", type=" + account.type);
            nav_userName.setText(account.name.split("@")[0]);
            nav_userEmail.setText(account.name);
            break;
        }
    }

    private void createNewGameAlertDialog() {
        L.d();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View v = inflater.inflate(R.layout.dialog_newgame, null);
        builder.setView(v)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText username = (EditText) (v.findViewById(R.id.username));
                        L.d(username.getText().toString());
                        badmintonGameName = username.getText().toString();
                        PreferenceUtils.setValue(mContext, badmintonGameNameKey, badmintonGameName);
                        initFirebase();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();

    }

    private void joinGameAlertDialog(final CharSequence[] items) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.choiceGame)
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        myFirebaseRef.child(fireBaseBadmintonKey).removeEventListener(getGameListValueEventListener);
                        L.d("which=" + which + ", items[which]=" + items[which]);
                        badmintonGameName = items[which].toString();
                        PreferenceUtils.setValue(mContext, badmintonGameNameKey, badmintonGameName);
                        initFirebase();
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    private void joinGame() {
        L.d();
        getGameList();
    }

    private void getGameList() {
        myFirebaseRef.child(fireBaseBadmintonKey).addValueEventListener(getGameListValueEventListener);
    }

    ValueEventListener getGameListValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            L.d("onDataChange: " + badmintonGameName + ", " + snapshot.getValue());
            L.d("snapshot.getChildrenCount()=" + snapshot.getChildrenCount());
            ArrayList<String> list = new ArrayList<String>();
            for (DataSnapshot ds : snapshot.getChildren()) {
                L.d("ds.getKey()=" + ds.getKey());
                list.add(ds.getKey());
            }
            joinGameAlertDialog(list.toArray(new CharSequence[list.size()]));
        }

        @Override
        public void onCancelled(FirebaseError error) {
            L.d("onCancelled");
        }
    };


    private boolean doDataBack() {
        DateInfo mDateInfo = mHistoryData.back();
        if (mDateInfo == null) {
            return false;
        } else {
            if (score1_num != mDateInfo.DataA) {
                score1_num = mDateInfo.DataA;
                myFirebaseRef.child(fireBaseBadmintonKey).child(badmintonGameName).child(score1Key).setValue(score1_num);
            }
            if (score2_num != mDateInfo.DataB) {
                score2_num = mDateInfo.DataB;
                myFirebaseRef.child(fireBaseBadmintonKey).child(badmintonGameName).child(score2Key).setValue(score2_num);
            }
            if (lastUpdate != mDateInfo.lastUpdate) {
                lastUpdate = mDateInfo.lastUpdate;
                myFirebaseRef.child(fireBaseBadmintonKey).child(badmintonGameName).child(lastUpdateKey).setValue(lastUpdate);
            }
            displayScore();
            return true;
        }
    }


    private void ttsSpeak(String text) {
        if(useTTS && tts!=null) {
            tts.setLanguage(Locale.CHINESE);
            tts.setSpeechRate(0.4f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "badminton");
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    private String getVersionName() {
        String appVersion = null;
        PackageManager manager = this.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            appVersion = info.versionName; //版本名
        } catch (PackageManager.NameNotFoundException e) {
            L.e(e);
        }
        return appVersion;
    }


    private void shotScreenAndShare(View view) {
        try {
            String path = StorageDirectory.getStorageDirectory(this, StorageDirectory.StorageType.ST_SDCard_RootDir) + "/Pictures/";
            File shotScreenFile = new File(path + "tmp_badminton.jpg");
            L.d("" + Uri.fromFile(shotScreenFile));
            Bitmap shotScreen = PictureManager.screenShot(view);
            StorageDirectory.checkPath(path);
            PictureManager.saveBitmapToFile(shotScreen, shotScreenFile);
            PictureManager.shareURI(mContext, Uri.fromFile(shotScreenFile));
        } catch (Exception e) {
            L.e(e.getMessage());
        }
    }



    class HistoryData {

        private ArrayList<DateInfo> updateInfoArray = new ArrayList<DateInfo>();

        private boolean checkLastData(DateInfo mDateInfo) {
            int size = updateInfoArray.size();
            if (size > 0) {
                DateInfo dateInfo = updateInfoArray.get(size - 1);
                if (dateInfo.equals(mDateInfo)) {
                    return true;
                }
            }
            return false;
        }

        public boolean add(DateInfo mDateInfo) {
            if (!checkLastData(mDateInfo)) {
                updateInfoArray.add(mDateInfo);
                return true;
            }
            return false;
        }

        public DateInfo back() {
            int size = updateInfoArray.size();
            if (size > 1) {
                DateInfo mDateInfo = updateInfoArray.get(size - 2);
                updateInfoArray.remove(size - 1);
                return mDateInfo;
            }
            return null;
        }

        public void clear() {
            updateInfoArray = new ArrayList<DateInfo>();
        }

        public int size() {
            return updateInfoArray.size();
        }
    }

    class DateInfo {
        long DataA = 0;
        long DataB = 0;
        String lastUpdate = "A";

        public DateInfo(long dataA, long dataB, String lastUpdate) {
            DataA = dataA;
            DataB = dataB;
            this.lastUpdate = lastUpdate;
        }

        public boolean equals(DateInfo o) {
            //if (this.DataA == o.DataA && this.DataB == o.DataB && this.lastUpdate.equals(o.lastUpdate)) {
            if (this.DataA == o.DataA && this.DataB == o.DataB ) {
                return true;
            } else {
                return super.equals(o);
            }
            //return super.equals(o);
        }
    }



    public final <E extends View> E
    getView(int id) {
        return (E) findViewById(id);
    }
}
