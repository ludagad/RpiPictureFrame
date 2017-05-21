package com.example.rpisftp;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class MainActivity extends Activity {

	Uri imageUri = null;
	ArrayList<Uri> imageUris;
    UploadAsyncTask myUploadAsyncTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
				.add(R.id.container, new PlaceholderFragment())
				.commit();
		}

		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
			if (type.startsWith("image/")) {
				imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (imageUris != null) {
                    // Update UI to reflect multiple images being shared
                    //File myFile = new File(imageUri.getPath());
                    myUploadAsyncTask = new UploadAsyncTask(this);
                    myUploadAsyncTask.execute(imageUris);
                }
			}
		} else if (Intent.ACTION_SEND.equals(action) && type != null) {
			if (type.startsWith("image/")) {
				imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                ArrayList<Uri> dummyImageUris = new ArrayList<Uri>();
                if (imageUri != null) {
                    // Update UI to reflect  image being shared
                    myUploadAsyncTask = new UploadAsyncTask(this);
                    dummyImageUris.add(imageUri);
                    myUploadAsyncTask.execute(dummyImageUris);
                }
			}
		} else {
            finish();
        }
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    public void FinishAfterAsyncTask()
    {
        this.finish();
    }

}
