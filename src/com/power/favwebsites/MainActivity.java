//MainActivity.java
//save, store, and delete your favorite sites
package com.power.favwebsites;

import java.util.ArrayList;
import java.util.Collections;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ListActivity {
	
	//need to change this to URLs
	private static final String SEARCHES = "searches";
	
	//user entered url
	private EditText urlEditText;
	//user entered site name or nickname
	private EditText nameEditText;
	//favorite sites
	private SharedPreferences saveSites;
	//list of site urls
	private ArrayList<String> urls;
	//bind urls to ListView
	private ArrayAdapter<String> adapter;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//get references to editTexts
		urlEditText = (EditText) findViewById(R.id.urlEditText);
		nameEditText = (EditText) findViewById(R.id.nameEditText);
		
		//get sharedpreferences containting saved sites
		saveSites = getSharedPreferences(SEARCHES, MODE_PRIVATE);
		
		//store saved sites in ArrayList, then sort them
		urls = new ArrayList<String>(saveSites.getAll().keySet());
		Collections.sort(urls, String.CASE_INSENSITIVE_ORDER);
		
		//create ArrayAdapter and use it to bind urls to ListView
		adapter = new ArrayAdapter<String>(this, R.layout.list_item, urls);
		setListAdapter(adapter);
		
		//register listener to save new site
		ImageButton saveButton = (ImageButton) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(saveButtonListener);
		
		//register listener that launches the browser when user touches site entry
		getListView().setOnItemClickListener(itemClickListener);;
		
		//register listener that allows user to edit or delete a site entry
		getListView().setOnItemLongClickListener(itemLongClickListener);
	}
	
	//saveButtonListener saves url-name pair into SharedPreferences
	public OnClickListener saveButtonListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			//create entry if both editTexts are not empty
			if(urlEditText.getText().length() > 0 && nameEditText.getText().length() > 0)
			{
				addNewFavSite(urlEditText.getText().toString(), nameEditText.getText().toString());
				//clear edit text fields
				urlEditText.setText("");
				nameEditText.setText("");
				
				((InputMethodManager) getSystemService(
						Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
								nameEditText.getWindowToken(), 0);
			} 
			//otherwise prompt for input
			else {
				//create a new AlertDialogue Builder
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				
				//set dialog's title and message
				builder.setMessage(R.string.missingMessage);
				
				//provide ok button to dismiss dialog
				builder.setPositiveButton(R.string.OK, null);
				
				//create AlertDialog from AlertDialog.builder
				AlertDialog errorDialog = builder.create();
				errorDialog.show();
			}//close if/else
		}//close onClick
	}; //end onclick listener

	//add new site to the save file, then refresh all buttons
	private void addNewFavSite(String url, String siteName)
	{
		//get a SharedPreferences.Editor to store new url/name pair
		SharedPreferences.Editor preferencesEditor = saveSites.edit();
		//store new url/siteName
		preferencesEditor.putString(url, siteName);
		//store updated preferences
		preferencesEditor.apply();
		
		//if siteName is new, add to and sort urls, then display updated list
		if(!urls.contains(siteName))
		{
			//add new url
			urls.add(url);
			//resort urls of sites
			Collections.sort(urls, String.CASE_INSENSITIVE_ORDER);
			//rebind urls to ListView
			adapter.notifyDataSetChanged();
		}
	}

	//itemClickListener launches web browser to go to saved site
	OnItemClickListener itemClickListener = new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			//get URL string from saveSites
			String url = ((TextView) view).getText().toString();
			String urlString = Uri.encode(saveSites.getString(url, ""), "UTF-8");
			
			//create an intent to launch browser
			Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
			
			//launch browser
			startActivity(webIntent);
		}
	};//end itemClickListener
	
	//itemLongClickListener allows users to delete or edit saved entries
	OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener()
	{
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
		{
			//get the url that the user long touched
			final String url = ((TextView) view).getText().toString();
			
			//create a new alertdialog
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			
			//set Alertdialog's title
			builder.setTitle(getString(R.string.shareEditDeleteTitle, url));
			
			//set list of itmes to display in dialog
			builder.setItems(R.array.dialog_items, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					switch (which)
					{
					case 0: //share
						shareURL(url);
						break;
					case 1: //edit
						//set editTexts to match chose url and name
						urlEditText.setText(url);
						break;
					case 2: //delete
						deleteUrl(url);
						break;
					}//end switch
				}//end onClick
			}//end dialog interface
			);//end call to builder.setitems
			
			//set alertdialog's negative button
			builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
			{
				//called when user hit cancels 
				public void onClick(DialogInterface dialog, int id)
				{
					dialog.cancel(); //dismiss alert dialog
				}
			}
			
			);
			
			//display alert dialog
			builder.create().show();
			return true;
		
		}
	};//end onitemlongclicklistener
	
	//allows user to choose an app for sharing a url
	private void shareURL(String url)
	{
		//get encoded url
		String urlString = Uri.encode(saveSites.getString(url, ""), "UTF-8");
		
		//create intent to share urlString
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shareSubject));
		shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.shareMessage, urlString));
		shareIntent.setType("text/plain");
		
		//display apps that can share text
		startActivity(Intent.createChooser(shareIntent, getString(R.string.shareUrl))); 
	}
	
	//deletes url after user confirms delete operation
	private void deleteUrl(final String url)
	{
		//create new alert dialog
		AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
		
		//set alert dialog's message
		confirmBuilder.setMessage(getString(R.string.confirmMessage, url));
		
		//set negative button for alert dialog
		confirmBuilder.setNegativeButton(getString(R.string.cancel), 
				new DialogInterface.OnClickListener()
				{
					//called when user selects "cancel"
					public void onClick(DialogInterface dialog, int id)
					{
						//dismiss dialog
						dialog.cancel();
					}
				}
		
				);//end setNegative
		
		//set positive button for alert dialog
		confirmBuilder.setPositiveButton(getString(R.string.delete),
				new DialogInterface.OnClickListener()
				{
					//called when delete button is clicked
					public void onClick(DialogInterface dialog, int id)
					{
						//delete url from urls list
						urls.remove(url);
						
						//get SharedPreferences.editor to remove url
						SharedPreferences.Editor preferencesEditor = saveSites.edit();
						preferencesEditor.remove(url);
						preferencesEditor.apply();
						
						//rebind urls array list to list view to show updated list
						adapter.notifyDataSetChanged();
					}
				}
				
				);
		
		//display alertdialog
		confirmBuilder.create().show();
		
		
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
