/**
 * eGov suite of products aim to improve the internal efficiency,transparency, accountability and the service delivery of the
 * government organizations.
 * 
 * Copyright (C) <2015> eGovernments Foundation
 * 
 * The updated version of eGov suite of products as by eGovernments Foundation is available at http://www.egovernments.org
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/ or http://www.gnu.org/licenses/gpl.html .
 * 
 * In addition to the terms of the GPL license to be adhered to in using this program, the following additional terms are to be
 * complied with:
 * 
 * 1) All versions of this program, verbatim or modified must carry this Legal Notice.
 * 
 * 2) Any misrepresentation of the origin of the material is prohibited. It is required that all modified versions of this
 * material be marked in reasonable ways as different from the original version.
 * 
 * 3) This license does not grant any rights to any user of the program with regards to rights under trademark law for use of the
 * trade names or trademarks of eGovernments Foundation.
 * 
 * In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.android.view.activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.egov.android.AndroidLibrary;
import org.egov.android.R;
import org.egov.android.api.ApiResponse;
import org.egov.android.common.JSONUtil;
import org.egov.android.controller.ApiController;
import org.egov.android.listener.Event;
import org.egov.android.model.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class RegisterActivity extends BaseActivity {
	private JSONArray jsoncitiesarry=new JSONArray();
	SharedPreferences sharedpreferences;

    /**
     * It is used to initialize an activity. An Activity is an application component that provides a
     * screen with which users can interact in order to do something, To initialize the
     * RegisterActivity.Set click listener to the save button.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ((Button) findViewById(R.id.register_doRegister)).setOnClickListener(this);
        Spinner citydropdown = (Spinner) findViewById(R.id.citydropdown);
        
        if(isInternetAvailable())
        {
        	new getCitiesFromURL(RegisterActivity.this, citydropdown).execute(AndroidLibrary.getInstance().getConfig()
					.getString("app.citiesJsonUrl"));
        }
        else
        {
        	Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_LONG).show();
        	this.finish();
        }
        
        sharedpreferences = getApplicationContext().getSharedPreferences("eGovPreference", 0); // 0 - for private mode
        showPasswordConstraintMessage();
    }
    
    /**
     * internet connection test function
     */
    public boolean isInternetAvailable()
    {
    	ConnectivityManager connectivity = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean available = false;
        if (netInfo.isConnected()) {
            available = true;
        } else {
            netInfo = connectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            available = netInfo.isConnected();
        }
        return available;
    }
    
    /**
     * async task for getting cities list from httprequest
     */
    
    class getCitiesFromURL extends AsyncTask<String, Void, String>
    {
    	
    	ProgressDialog mDialog = null;
    	Spinner citydropdown=null;
    	Context context=null;
    	
    	public getCitiesFromURL(Context context, Spinner citydropdown) {
			// TODO Auto-generated constructor stub
    		this.context=context;
    		this.citydropdown=citydropdown;
		}
    	
    	@Override
    	protected void onPreExecute() {
    		// TODO Auto-generated method stub
    		super.onPreExecute();
    		mDialog=new ProgressDialog(context);
    		mDialog.setMessage("Please wait...");
            mDialog.setCancelable(false);
            mDialog.show();
    	}
    	
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			return getJSON(params[0]);
		}
    	
		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
			if(mDialog!=null)
			{
				mDialog.dismiss();
			}
			
			try {
				if(!result.equals("ERROR"))
				{
				 jsoncitiesarry=new JSONArray(result);
				 loadCitiesFromJsonArray(citydropdown);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Toast.makeText(context, "Something went wrong in application!", Toast.LENGTH_LONG).show();
			}
			
		}
    	
    	
    }
    
    
    /**
     * load spinner from jsonarray
     * @throws JSONException 
     */
    
    
    public void loadCitiesFromJsonArray(Spinner dropdown) throws JSONException
    {
    	
    	List<String> spinnerArray =  new ArrayList<String>();
    	
    	//default option
    	spinnerArray.add("Select City");
    	
    	//sort cities a to z
    	jsoncitiesarry = JSONUtil.sort(jsoncitiesarry, new Comparator(){
    		   public int compare(Object a, Object b){
    		      JSONObject    ja = (JSONObject)a;
    		      JSONObject    jb = (JSONObject)b;
    		      return ja.optString("city_name", "").toLowerCase().compareTo(jb.optString("city_name", "").toLowerCase());
    		   }
    	});
    	
    	for(int i=0; i<jsoncitiesarry.length(); i++)
    	{
    		JSONObject city=jsoncitiesarry.getJSONObject(i);
    		spinnerArray.add(city.getString("city_name"));
    	}
    	
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
           this, android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdown.setAdapter(adapter);

    }
    
    
    /**
     * Get cities list json from app config url
     */
    
    public String getJSON(String address){
    	StringBuilder builder = new StringBuilder();
    	HttpClient client = new DefaultHttpClient();
    	HttpGet httpGet = new HttpGet(address);
    	try{
    		
    		HttpParams params=client.getParams();
    		HttpConnectionParams.setConnectionTimeout(params, (60*1000));
    		HttpResponse response = client.execute(httpGet);
    		StatusLine statusLine = response.getStatusLine();
    		int statusCode = statusLine.getStatusCode();
    		if(statusCode == 200){
    			HttpEntity entity = response.getEntity();
    			InputStream content = entity.getContent();
    			BufferedReader reader = new BufferedReader(new InputStreamReader(content));
    			String line;
    			while((line = reader.readLine()) != null){
    				builder.append(line);
    			}
    		} else {
    			Log.e(RegisterActivity.class.toString(),"Failedet JSON object");
    		}
    	}catch(ClientProtocolException e){
    		e.printStackTrace();
    		return "ERROR";
    	} catch (IOException e){
    		e.printStackTrace();
    		return "ERROR";
    	}
    	return builder.toString();
    }
    

    /**
     * Event triggered when clicking on the item having click listener. When user clicks on save
     * button _register() function get called.
     */
    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.register_doRegister:
                _register();
                break;
        }
    }

    /**
     * Function called when clicking on save button. Check the empty field validation and show the
     * message. If all fields are correct then call the register api to register a new citizen.
     */
    private void _register() {

        String name = ((EditText) findViewById(R.id.register_name)).getText().toString().trim();
        String phone = ((EditText) findViewById(R.id.register_phone)).getText().toString().trim();
        String email = ((EditText) findViewById(R.id.register_email)).getText().toString().trim();
        String password = ((EditText) findViewById(R.id.register_password)).getText().toString()
                .trim();
        String confirm_password = ((EditText) findViewById(R.id.register_confirm_password))
                .getText().toString().trim();
        String deviceId = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        Integer citySelectedIdx = ((Spinner) findViewById(R.id.citydropdown)).getSelectedItemPosition();

        if (isEmpty(name)) {
            showMessage(getMessage(R.string.name_empty));
            return;
        } else if (name.length() < 3) {
            showMessage(getMessage(R.string.name_length));
            return;
        } else if (isEmpty(phone)) {
            showMessage(getMessage(R.string.phone_empty));
            return;
        } else if (phone.length() != 10) {
            showMessage(getMessage(R.string.phone_number_length));
            return;
        } else if (isEmpty(email)) {
            showMessage(getMessage(R.string.email_empty));
            return;
        } else if (!_isValidEmail(email)) {
            showMessage(getMessage(R.string.invalid_email));
            return;
        } else if (isEmpty(password)) {
            showMessage(getMessage(R.string.password_empty));
            return;
        } else if (!_isValidPassword(password)) {
        	showPasswordConstraintMessage();
            return;
        } else if (isEmpty(confirm_password.toString())) {
            showMessage(getMessage(R.string.confirm_password_empty));
            return;
        } else if (!password.equals(confirm_password)) {
            showMessage(getMessage(R.string.password_match));
            return;
        } else if (citySelectedIdx == 0) {
            showMessage(getMessage(R.string.city_selection_empty));
            return;
        }
        

        User user = new User();
        user.setName(name);
        user.setMobileNo(phone);
        user.setEmail(email);
        user.setPassword(password);
        user.setConfirmPassword(confirm_password);
        user.setDeviceId(deviceId);
        
        try {
			ApiController.getInstance().register(this, user, getValidURL(jsoncitiesarry.getJSONObject((citySelectedIdx-1)).getString("url")));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Toast.makeText(RegisterActivity.this, "Error Occured While Registering!", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
        
    }
    
    private void showPasswordConstraintMessage()
    {
    	Toast toast = Toast
                .makeText(
                        this,
                        "Password must be at least 8 to 32 characters long and must have one or more upper case and lower case alphabet,number and special character except \'& < > # % \" ' / \\' and space",
                        2000);
        toast.setGravity(Gravity.TOP, 0, 120);
        toast.show();
    }
    
    private String getValidURL(String url)
    {
       return (url.endsWith("/")? url: url+"/");
    }

    private boolean _isValidPassword(String password) {
        String expression = "((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@$;:+=-_?()!]).{8,32})";
        Pattern pattern = Pattern.compile(expression);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    /**
     * Function used to check whether the entered mail id is valid or not
     * 
     * @param email
     *            => mail id entered by user
     * @return
     */
    private boolean _isValidEmail(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches() ? true : false;
    }

    /**
     * Function used to clear the fields once user successfully registered
     */
    private void _clearAllText() {
        ((EditText) findViewById(R.id.register_name)).setText("");
        ((EditText) findViewById(R.id.register_phone)).setText("");
        ((EditText) findViewById(R.id.register_email)).setText("");
        ((EditText) findViewById(R.id.register_password)).setText("");
        ((EditText) findViewById(R.id.register_confirm_password)).setText("");
    }

    /**
     * The onResponse method will be invoked after the Register activation API call onResponse
     * methods will contain the response If the response has status as 'success' then onResponse
     * contains the JSON object. The JSONObject is handled in the onResponse method.finally
     * _clearAllText() function to reset the fields. then redirect to AccountActivationActivity and
     * pass the email/phone through intent.
     */
    @Override
    public void onResponse(Event<ApiResponse> event) {
        super.onResponse(event);
        String msg = event.getData().getApiStatus().getMessage();
        String status = event.getData().getApiStatus().getStatus();
        
        if (status.equalsIgnoreCase("success")) {
            try {
            	
                Integer citySelectedIdx = ((Spinner) findViewById(R.id.citydropdown)).getSelectedItemPosition();
            	Editor editor = sharedpreferences.edit();
            	editor.putString("api.baseUrl", getValidURL(jsoncitiesarry.getJSONObject((citySelectedIdx-1)).getString("url")));
            	editor.commit();
            	
                JSONObject jo = new JSONObject(event.getData().getResponse().toString());
                Intent intent = new Intent(this, AccountActivationActivity.class);
                intent.putExtra("username", jo.getString("userName"));
                intent.putExtra("password", ((EditText) findViewById(R.id.register_password))
                        .getText().toString().trim());
                startActivity(intent);
                _clearAllText();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            showMessage(msg);
        }
        else
        {
        	showMessage("This Mobile No or Email Address Already Registered!");
        }
    }
}