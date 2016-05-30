/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.lison.android.gank.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import cc.lison.android.gank.adapter.MainFragmentPagerAdapter;
import cc.lison.android.gank.config.Constants;
import cc.lison.android.gank.data.ImageGoodsCache;
import cc.lison.android.gank.db.Image;
import cc.lison.android.gank.model.GoodsResult;
import cc.lison.android.gank.network.GankCloudApi;
import com.orhanobut.logger.Logger;
import com.umeng.analytics.MobclickAgent;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    @Bind(cc.lison.android.gank.R.id.toolbar)
    Toolbar mToolbar;
    @Bind(cc.lison.android.gank.R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @Bind(cc.lison.android.gank.R.id.viewpager)
    ViewPager mViewPager;
    @Bind(cc.lison.android.gank.R.id.tabs)
    TabLayout mTabLayout;
    @Bind(cc.lison.android.gank.R.id.nav_view)
    NavigationView mNavigationView;
    @Bind(cc.lison.android.gank.R.id.fab)
    FloatingActionButton mFABtn;

    private Realm mRealm;
    private Bundle mReenterState;
    private MainFragmentPagerAdapter mPagerAdapter;
    private BenefitListFragment mBenefitListFragment;

    /***
     * 获取福利图的回调接口，拿到数据用来做背景
     */
    private Observer<GoodsResult> getImageGoodsObserver = new Observer<GoodsResult>() {
        @Override
        public void onNext(final GoodsResult goodsResult) {
            if (null != goodsResult && null != goodsResult.getResults()) {
                ImageGoodsCache.getIns().addAllImageGoods(goodsResult.getResults());
            }
        }

        @Override
        public void onCompleted() {
            Logger.d("获取背景图服务完成");
        }

        @Override
        public void onError(final Throwable error) {
            Logger.e(error,"获取背景图服务失败");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(cc.lison.android.gank.R.layout.activity_main);

        mRealm = Realm.getInstance(this);

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        final ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(cc.lison.android.gank.R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);

        setupDrawerContent(mNavigationView);
        setupViewPager();

        mTabLayout.setupWithViewPager(mViewPager);

        loadAllImageGoods();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private void setupViewPager() {
        mBenefitListFragment = new BenefitListFragment();
        mPagerAdapter = new MainFragmentPagerAdapter(getSupportFragmentManager());
        //mPagerAdapter.addFragment(CommonGoodsListFragment.newFragment("Android"), "Android");
        //mPagerAdapter.addFragment(CommonGoodsListFragment.newFragment("IOS"), "IOS");
        mPagerAdapter.addFragment(mBenefitListFragment, "福利");
        mViewPager.setAdapter(mPagerAdapter);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.getMenu().findItem(cc.lison.android.gank.R.id.nav_home).setChecked(true);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        disposeMenuAction(menuItem);
                        return true;
                    }
                });
    }

    private void disposeMenuAction(MenuItem item){
        switch (item.getItemId()){
//            case cc.lison.android.gank.R.id.nav_collect:
//            case cc.lison.android.gank.R.id.nav_time:
//                Toast.makeText(this,"功能开发中",Toast.LENGTH_SHORT).show();
//                break;
//            case cc.lison.android.gank.R.id.nav_code:
//                callWebView(Constants.GITHUB_URL);
//                break;
            case cc.lison.android.gank.R.id.nav_author:
                callWebView(Constants.AUTHOR_URL);
                break;
        }
    }

    private void loadAllImageGoods() {
        RealmResults<Image> allImage = mRealm.where(Image.class).findAll();
        if (allImage.size() == 0) {
            GankCloudApi.getIns()
                    .getBenefitsGoods(GankCloudApi.LOAD_LIMIT, 1)
                    .cache()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(getImageGoodsObserver);
        } else {
            ImageGoodsCache.getIns().addAllImageGoods(allImage);
        }
    }

    private void callWebView(String url){
        Intent intent= new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        startActivity(intent);
    }
}
