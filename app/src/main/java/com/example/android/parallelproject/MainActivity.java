package com.example.android.parallelproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.parallelproject.data.earthQuakeDbHelper;
import com.example.android.parallelproject.data.earthQuakeContract.earthQuakeEntry;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    /** URL for earthquake data from the USGS dataset from 1-1-2020 to 1-10-2020*/
    private static final String[] USGS_REQUEST_URLS =
            {"https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2020-01-01&endtime=2020-10-1&minmagnitude=1&maxmagnitude=3&limit=10",
            "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2020-01-01&endtime=2020-10-1&minmagnitude=4.7&maxmagnitude=6&limit=10",
            "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2020-01-01&endtime=2020-10-1&minmagnitude=6.9"};

    earthQuakeDbHelper mDbHelper = new earthQuakeDbHelper(this);
    SQLiteDatabase mDb;

    // Create a fake list of earthquakes.
    ArrayList<earthQuake> earthquakes = new ArrayList<>() , quakes1,quakes2,quakes3;

    // Find a reference to the ListView in the layout
    ListView earthquakeListView;

    // Find a reference to the buttons in the layout
    Button requestButton;
    Button saveButton;

    /** Projection for database queries */
    String[] projection = new String[] {
            earthQuakeEntry.COLUMN_MAGNITUDE,
            earthQuakeEntry.COLUMN_DATE,
            earthQuakeEntry.COLUMN_LOCATION,
            earthQuakeEntry.COLUMN_URL
    };
    /** Object to store data from database.query() method */
    Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestButton = findViewById(R.id.request_button);
        saveButton = findViewById(R.id.save_button);
        saveButton.setEnabled(false);
        Button displayButton = findViewById(R.id.display_button);
        earthquakeListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        earthquakeListView.setEmptyView(emptyView);

        if(getCount() == 0)
            displayButton.setEnabled(false);
        else
            requestButton.setEnabled(false);

        /** set onclicklistener for each item in the listview to open the url specified for each earthquake*/
        earthquakeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                earthQuake quake =earthquakes.get(position);

                Uri webpage = Uri.parse(quake.getURL());
                Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        requestButton.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   requestButton.setEnabled(false);
                   sendRequests();
                   Toast.makeText(MainActivity.this, "Data has been received from the api", Toast.LENGTH_SHORT).show();
                   saveButton.setEnabled(true);
               }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread() {
                    public void run() {
                        insertData(quakes1);
                    }
                }.start();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                new Thread() {
                    public void run() {
                        insertData(quakes2);
                    }
                }.start();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                new Thread() {
                    public void run() {
                        insertData(quakes3);
                    }
                }.start();

                Toast.makeText(getApplicationContext(), R.string.data_saved, Toast.LENGTH_SHORT).show();
                saveButton.setEnabled(false);
                displayButton.setEnabled(true);
            }
        });

        displayButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                updateUi();
            }
        });
    }

    /** send requests to the api*/
    public void sendRequests()
    {
        ApiThread request1 = new ApiThread(USGS_REQUEST_URLS[0]);
        request1.start();

        ApiThread request2 = new ApiThread(USGS_REQUEST_URLS[1]);
        request2.start();

        ApiThread request3 = new ApiThread(USGS_REQUEST_URLS[2]);
        request3.start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        quakes1 = Utilites.extractEarthquakes(request1.getResponse());
        quakes2 = Utilites.extractEarthquakes(request2.getResponse());
        quakes3 = Utilites.extractEarthquakes(request3.getResponse());
    }

    public int getCount()
    {
        mDb = mDbHelper.getReadableDatabase();

        cursor = mDb.query(earthQuakeEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null);
        return cursor.getCount();
    }

    /**
     * Insert the earthquakes data into the database
     */
    public void insertData(ArrayList<earthQuake> quakes)
    {
        mDb = mDbHelper.getWritableDatabase();
        int i;
        ContentValues values = new ContentValues();
        for(i = 0; i < quakes.size(); i++)
        {
            values.put(earthQuakeEntry.COLUMN_MAGNITUDE,quakes.get(i).getMagnitude());
            values.put(earthQuakeEntry.COLUMN_LOCATION,quakes.get(i).getLocation());
            values.put(earthQuakeEntry.COLUMN_DATE,quakes.get(i).getTimeInMilliseconds());
            values.put(earthQuakeEntry.COLUMN_URL,quakes.get(i).getURL());
            mDb.insert(earthQuakeEntry.TABLE_NAME,null,values);
        }
    }


    /**
     * Update the UI with the earthquakes Arraylist.
     */
    public void updateUi()
    {
        // load earthquakes from database to earthquakes arraylist
        loadDataFromDb();

        // attach the earthquakes arraylist to the adapter
        earthQuakeAdapter adapter = new earthQuakeAdapter(this,earthquakes);

        // Find a reference to the {@link ListView} in the layout
        ListView earthquakeListView = (ListView) findViewById(R.id.list);

        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        earthquakeListView.setAdapter(adapter);
    }

    /**
     * Load earthquakes data from the database.
     */

    public void loadDataFromDb()
    {
        earthquakes = new ArrayList<>();
        mDb = mDbHelper.getReadableDatabase();

        cursor = mDb.query(earthQuakeEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null);

        // Toast to inform the user that the data being loaded from the database
        Toast.makeText(this, R.string.loading_data, Toast.LENGTH_SHORT).show();

        int magnitudeColumnindex = cursor.getColumnIndex(earthQuakeEntry.COLUMN_MAGNITUDE);
        int locationColumnindex = cursor.getColumnIndex(earthQuakeEntry.COLUMN_LOCATION);
        int dateColumnindex = cursor.getColumnIndex(earthQuakeEntry.COLUMN_DATE);
        int urlColumnindex = cursor.getColumnIndex(earthQuakeEntry.COLUMN_URL);

        while(cursor.moveToNext())
        {
            earthquakes.add(new earthQuake(cursor.getFloat(magnitudeColumnindex),
                    cursor.getString(locationColumnindex),
                    cursor.getLong(dateColumnindex),
                    cursor.getString(urlColumnindex)));
        }

        // Always close the cursor when you're done reading from it. This releases all its
        // resources and makes it invalid.
        cursor.close();

    }

}