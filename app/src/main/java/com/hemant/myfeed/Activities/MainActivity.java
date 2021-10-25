package com.hemant.myfeed.Activities;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.ads.MobileAds;
import com.hemant.myfeed.R;
import com.hemant.myfeed.fragments.BlankFragment;
import com.hemant.myfeed.fragments.MainFragment;
import com.hemant.myfeed.model.Topic;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;
import com.zplesac.connectionbuddy.ConnectionBuddy;
import com.zplesac.connectionbuddy.cache.ConnectionBuddyCache;
import com.zplesac.connectionbuddy.interfaces.ConnectivityChangeListener;
import com.zplesac.connectionbuddy.models.ConnectivityEvent;
import com.zplesac.connectionbuddy.models.ConnectivityState;

import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmQuery;



public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        BlankFragment.OnFragmentInteractionListener,
        MainFragment.OnFragmentInteractionListener,
        ConnectivityChangeListener{

    android.support.v4.app.FragmentTransaction fragmentTransaction;

    private Realm realm;
    ProgressDialog progressDialog;
    private final Handler mHandler = new Handler();

    MainFragment mainFragment = new MainFragment();
    String url = "http://www.majorgeeks.com/news/rss/news.xml";
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    DrawerLayout drawer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id
                .cordinatorlayout);
        if(savedInstanceState != null){
            ConnectionBuddyCache.clearLastNetworkState(this);
        }
        ButterKnife.bind(this);
        MobileAds.initialize(this, "ca-app-pub-4083824684818051~6248546920");
        ConnectionBuddy.getInstance().registerForConnectivityEvents(this, this);
        Realm.init(getApplicationContext());
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder().name(Realm.DEFAULT_REALM_NAME)
                .schemaVersion(0)
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(realmConfiguration);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Fetching");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.flipy);
        rotation.setFillAfter(true);
        progressDialog.show();
        setSupportActionBar(toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        if(!isOnline()){
            Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, "No Internet !!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Settings", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    });

            snackbar.show();

            progressDialog.dismiss();

        }

        getFromFirebase();
        if (realm.where(Topic.class).findAll().size() > 0) {

            progressDialog.dismiss();
        }
            showHomeFragment();



    }
    public  boolean checkIfExists(String id){

        RealmQuery<Topic> query = realm.where(Topic.class)
                .equalTo("topic", id);

        return query.count() != 0;
    }
    public void getFromFirebase(){

        Firebase myFirebaseRef = new Firebase("https://knowfeed.firebaseio.com/");

        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Log.wtf("onDataChange: ", postSnapshot.getKey());
                    HashMap<String, String> dummy = new HashMap<>();

                    for (DataSnapshot topicSnapshot : postSnapshot.getChildren()) {
                        dummy.put(topicSnapshot.getKey(), (String) topicSnapshot.getValue());

                    }

                    realm.beginTransaction();
                    realm.copyToRealmOrUpdate(new Topic(realm, postSnapshot.getKey(), dummy));
                    realm.commitTransaction();
                }
                    if(mainFragment.isVisible() ){
                        progressDialog.dismiss();
                        mainFragment.reloadList();
                    }
            }

            @Override
            public void onCancelled(FirebaseError error) {

            }
        });
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }
        return (networkInfo != null && networkInfo.isConnected());
    }
    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (mainFragment.isVisible()) {
                this.finish();
            } else {
                showHomeFragment();
            }
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
           showHomeFragment();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
public void showFragment(Fragment fragmentToSet){
    fragmentTransaction = getSupportFragmentManager().beginTransaction();
    fragmentTransaction.replace(R.id.frame, fragmentToSet);
    fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
            android.R.anim.fade_in, android.R.anim.fade_out);
    fragmentTransaction.commit();
}
    public void setUrl(String mUrl){
        showFragment(BlankFragment.newInstance(mUrl)
        );

    }
public void showHomeFragment(){
    if (mainFragment == null){
        mainFragment = new MainFragment();
    }
    toolbar.setTitle("NEWS");
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frame, mainFragment).addToBackStack("mainFragnment");
        fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);
        fragmentTransaction.commitAllowingStateLoss();

    }

    @Override
    public void onConnectionChange(ConnectivityEvent event) {
        if(event.getState() == ConnectivityState.CONNECTED){

        }
        else{
            final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id
                    .cordinatorlayout);
            Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, "No Internet", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Settings", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    });

            snackbar.show();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        ConnectionBuddy.getInstance().unregisterFromConnectivityEvents(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ConnectionBuddy.getInstance().registerForConnectivityEvents(this, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
