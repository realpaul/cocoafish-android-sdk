package com.cocoafish.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.cocoafish.sdk.CCConstants;
import com.cocoafish.sdk.CCMeta;
import com.cocoafish.sdk.CCRequestMethod;
import com.cocoafish.sdk.CCResponse;
import com.cocoafish.sdk.CCUser;
import com.cocoafish.sdk.Cocoafish;
import com.cocoafish.sdk.CocoafishError;

public class UserView extends Activity {

    static final int LAUNCH_SIGNUP = 0;
//    List<CCResponse> listOfCheckin;
    private Cocoafish sdk;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sdk = DemoApplication.getSdk();
        try {
			if ( sdk.getCurrentUser() == null) {
				showLoginView();
			} else {
            	showUserView();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
  
    @Override
    public void onPause()
    {
    	super.onPause();
    	// Ugly solution to hide the soft keyboard by forcing it to show first, then toggle it
    	InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    	inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 
    			InputMethodManager.HIDE_IMPLICIT_ONLY); 
    	inputManager.toggleSoftInput(0, 0);
    }
    
    protected void performLogin() {
		final ProgressDialog dialog = new ProgressDialog(UserView.this);
		dialog.setMessage("Login...");
	    dialog.show();
	    
    	String login = ((EditText) findViewById(R.id.email_address)).getText().toString();
    	String password = ((EditText) findViewById(R.id.pw)).getText().toString();
    	String errorMsg = null;
    	try {
	    	HashMap<String, Object> dataMap = new HashMap<String, Object>();
	    	dataMap.put("login", login);
	    	dataMap.put("password", password);
	    	CCResponse response = sdk.sendRequest("users/login.json", CCRequestMethod.POST, dataMap, false);
	    	CCMeta meta = response.getMeta();
	    	if( meta.getCode() != CCConstants.SUCCESS_CODE )
	    			throw new CocoafishError(meta.getMessage());
	    	
	    	showUserView();
		} catch (CocoafishError e) {
			errorMsg = e.getMessage();
    	} 
		dialog.dismiss();
		
		if (errorMsg != null) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    		alertDialog.setTitle("Login Failed");
    		alertDialog.setMessage(errorMsg);
    		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
    		   public void onClick(DialogInterface dialog, int which) {
    		      // here you can add functions
    			   dialog.dismiss();
    		   }
    		});
    		alertDialog.setIcon(R.drawable.icon);
    		alertDialog.show();
		}
    }
    
    protected void performRefresh() {
    	new GetCheckinsTask().execute();
    }
    
    protected void performLogout() {
	    try {
			sdk.sendRequest("users/logout.json", CCRequestMethod.GET, null, false);
		} catch (CocoafishError e) {
			e.printStackTrace();
		}
		showLoginView();
    }
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == LAUNCH_SIGNUP) {
            if (resultCode == RESULT_OK) {
            	showUserView();
            }
        }
    }

    protected void showLoginView()
    {
		setContentView(R.layout.signin);

        View loginButton = findViewById(R.id.signin);
        loginButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		performLogin();
	    }});
        
        View signupButton = findViewById(R.id.signup);
        signupButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		startActivityForResult(new Intent(UserView.this, SignUp.class), LAUNCH_SIGNUP);
	    }});
    }
    
    protected void showUserView()
    {
        setContentView(R.layout.userview);
        View refreshButton = findViewById(R.id.refresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		performRefresh();
	    }});
        
        View logout = findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		performLogout();
        }});
       
        try {
            TextView name = (TextView)findViewById(R.id.UserName);
            CCUser user = sdk.getCurrentUser();
            if( user != null)
            	name.setText( user.getFirst() + " " + user.getLast() );
        } catch (Exception e) {
			e.printStackTrace();
        }
        performRefresh();
    }
  
    protected void showCheckins(List<JSONObject> checkins) {
        
    	if (checkins == null) {
    		return;
    	}
    	InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    		
        ListView list = (ListView)findViewById(R.id.CheckinListView);
        
        CheckinAdapter adapter = new CheckinAdapter(this, checkins, false);
        
        list.setAdapter(adapter);
    }
    
	private class GetCheckinsTask extends AsyncTask<Void, Void, List<JSONObject>> {
		private final ProgressDialog dialog = new ProgressDialog(UserView.this);
	    private String errorMsg = null;
	    protected void onPreExecute()
	    {
	        dialog.setMessage("Loading...");
	        dialog.show();
	    }
	    
	    protected void onPostExecute(List<JSONObject> checkins) {
	    	 if(this.dialog.isShowing())
	         {
	             this.dialog.dismiss();
	         }
	    	 if (errorMsg != null) {
	     		AlertDialog alertDialog = new AlertDialog.Builder(UserView.this).create();
	    		alertDialog.setTitle("Failed");
	    		alertDialog.setMessage(errorMsg);
	    		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
	    		   public void onClick(DialogInterface dialog, int which) {
	    		      // here you can add functions
	    			   dialog.dismiss();
	    		   }
	    		});
	    		alertDialog.setIcon(R.drawable.icon);
	    		alertDialog.show();
	    	 } else {
	    		 showCheckins(checkins);
	    	 }
	    }

		@Override
		protected List<JSONObject> doInBackground(Void...params) {
			List<JSONObject> checkinsList = new ArrayList<JSONObject>();
			try {
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("user_id", sdk.getCurrentUser().getObjectId() );
				CCResponse response = sdk.sendRequest("checkins/search.json", CCRequestMethod.GET, data, false);


				JSONObject responseJSON = response.getResponseData();
				CCMeta meta = response.getMeta();
				if("ok".equals(meta.getStatus()) 
				    && meta.getCode() == 200 
				    && "searchCheckins".equals(meta.getMethod())) {
				  try {
					JSONArray checkins = responseJSON.getJSONArray("checkins");
					for( int i = 0 ; i < checkins.length() ; i++ )
						checkinsList.add(checkins.getJSONObject(i) );
				  } catch (JSONException e) { }
				}
			} catch (CocoafishError e) {
				errorMsg = e.getLocalizedMessage();
			} 
			return checkinsList;
		}
	}

}
