package org.egov.employee.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.IntentCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.egov.employee.adapter.TasksAdapter;
import org.egov.employee.api.ApiController;
import org.egov.employee.controls.SlidingTabLayout;
import org.egov.employee.data.Task;
import org.egov.employee.interfaces.TasksItemClickListener;

import offices.org.egov.egovemployees.R;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class Homepage extends BaseActivity implements TasksItemClickListener {

    Toolbar toolbar;
    ViewPager pager;
    TasksAdapter adapter;
    SlidingTabLayout tabs;
    public RelativeLayout homePageLoader;
    public LinearLayout inboxEmptyInfo;

    public static int ACTION_UPDATE_REQUIRED=111;

    String WORK_FLOW_TYPE_COMPLAINT="Complaint";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        homePageLoader=(RelativeLayout)findViewById(R.id.homepageloader);
        homePageLoader.setVisibility(View.GONE);
        inboxEmptyInfo=(LinearLayout)findViewById(R.id.inboxemptyinfo);
        inboxEmptyInfo.setVisibility(View.GONE);

        // Assiging the Sliding Tab Layout View
        tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        //tabs.setDistributeEvenly(true); // To make the Tabs Fixed set this true, This makes the tabs Space Evenly in Available width

        tabs.setCustomTabView(R.layout.custom_tabstrip, R.id.tabtext, R.id.badgetext);

        // Setting Custom Color for the Scroll bar indicator of the Tab View
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.colorAccent);
            }
        });

        getWorkListCategory();

        showSnackBar("Welcome, "+preference.getName());
        //BadgeView bv1 = new BadgeView(this, ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(0));

    }


    public void getWorkListCategory()
    {

        String currentMethodName=Thread.currentThread().getStackTrace()[2].getMethodName();

        if(checkInternetConnectivity(Homepage.this, currentMethodName)) {
            inboxEmptyInfo.setVisibility(View.GONE);
            homePageLoader.setVisibility(View.VISIBLE);
            Call<JsonObject> jsonInboxCategoryList = ApiController.getAPI(getApplicationContext(), Homepage.this).inboxCategoryWithItemsCount(preference.getApiAccessToken());

            Callback<JsonObject> inboxCategoryListCallback = new Callback<JsonObject>() {

                @Override
                public void onResponse(Response<JsonObject> response, Retrofit retrofit) {
                    JsonObject respJson = response.body();
                    //load worklist categories from server response
                    setWorkListCategoryTabs(respJson.get("result").getAsJsonArray());
                }

                @Override
                public void onFailure(Throwable t) {
                    homePageLoader.setVisibility(View.GONE);
                }
            };
            jsonInboxCategoryList.enqueue(inboxCategoryListCallback);
        }

    }

    public void setWorkListCategoryTabs(JsonArray responseArray)
    {

        JsonArray jsonWorkflowTypes=new JsonArray();
        //disable workflow types except complaint
        for(int i=0;i<responseArray.size();i++)
        {
            JsonObject workflowType = responseArray.get(i).getAsJsonObject();
            if(workflowType.get("workflowtype").getAsString().equals(WORK_FLOW_TYPE_COMPLAINT))
            {
                jsonWorkflowTypes.add(workflowType);
                break;
            }
        }

        //if inbox is empty then show some notification
        inboxEmptyInfo.setVisibility((jsonWorkflowTypes.size()==0?View.VISIBLE:View.GONE));
        homePageLoader.setVisibility(View.GONE);

        // Creating The ViewPagerAdapter and Passing Fragment Manager, Titles fot the Tabs and Number Of Tabs.
        adapter =  new TasksAdapter(getSupportFragmentManager(), jsonWorkflowTypes, preference.getApiAccessToken());
        // Assigning ViewPager View and setting the adapter
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        // Setting the ViewPager For the SlidingTabsLayout
        tabs.setViewPager(pager);
    }


    @Override
    protected int getLayoutResource() {
        return R.layout.activity_homepage;
    }

    @Override
    public void onTaskItemClicked(Task clickedTask) {
        Intent openTaskScreen=new Intent(Homepage.this, ViewTask.class);
        openTaskScreen.putExtra("task", clickedTask);
        startActivityForResult(openTaskScreen, ACTION_UPDATE_REQUIRED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_homepage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            //clear current user access token from app preference in splashscreen with flag param
            Intent intent = new Intent(this, SplashScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("isLoggedOut", true);
            this.startActivity(intent);
            this.overridePendingTransition(0,0);
        }
        else if(id == R.id.action_refresh)
        {
            getWorkListCategory();
        }
        else if(id == R.id.action_search)
        {

            View menuButton = findViewById(R.id.action_search);
            int[] searchActionBarLocation = new int[]{0,0};
            // This could be called when the button is not there yet, so we must test for null
            if (menuButton != null) {
                // Found it! Do what you need with the button
                menuButton.getLocationInWindow(searchActionBarLocation);
                searchActionBarLocation[0] = searchActionBarLocation[0]+menuButton.getWidth() / 2;
                searchActionBarLocation[1] = searchActionBarLocation[1]+menuButton.getHeight() / 2;
            }


            Intent searchScreen=new Intent(Homepage.this, SearchableActivity.class);
            searchScreen.putExtra("xyLocations", searchActionBarLocation);
            startActivity(searchScreen);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_UPDATE_REQUIRED && resultCode == RESULT_OK) {
            showSnackBar("Complaint Successfully Updated!");
            getWorkListCategory();
        }

    }
}