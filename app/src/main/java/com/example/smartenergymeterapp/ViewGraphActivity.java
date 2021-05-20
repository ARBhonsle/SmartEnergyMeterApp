package com.example.smartenergymeterapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.example.smartenergymeterapp.BluetoothMainActivity.Parameters;

import java.util.ArrayList;


public class ViewGraphActivity extends AppCompatActivity {
    private static final String TAG = "ReadAndWriteSnippets";
    private ArrayList<Parameters> parameters;
    private LineGraphSeries<DataPoint> voltageSeries;
    private LineGraphSeries<DataPoint> currentSeries;
    final Context mcontext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_graph);

        GraphView graph = findViewById ( R.id.view_graph );
        voltageSeries = new LineGraphSeries<DataPoint>();
        currentSeries = new LineGraphSeries<DataPoint>();
        parameters = new ArrayList<>();
        graph.addSeries (voltageSeries);
        graph.addSeries (currentSeries);
        graph.onDataChanged ( false,false );
        Viewport viewport = graph.getViewport ();
        viewport.setYAxisBoundsManual ( true );
        viewport.setScrollable ( true );

        DatabaseReference mDatabase = FirebaseDatabase.getInstance ( ).getReference ( "metrics" );

        Button home = findViewById ( R.id.home );
        home.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent( ViewGraphActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        Query recentReading = mDatabase.orderByChild ( "unixTime" ).limitToFirst ( 50 );
        recentReading.addValueEventListener ( new ValueEventListener ( ) {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot postSnapshot: snapshot.getChildren ()){
                    Parameters params = postSnapshot.getValue (Parameters.class);
                    voltageSeries.appendData(new DataPoint( Double.parseDouble(params.unixTime),
                            Double.parseDouble(params.voltage)),true,50);
                    Toast.makeText ( mcontext, "New Parameter added" ,Toast.LENGTH_SHORT).show ();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GraphActivity", String.valueOf ( error.toException () ) );
                Toast.makeText ( mcontext, "Failed to load parameters" ,Toast.LENGTH_SHORT).show ();
            }
        } );

    }

    /*
    @Override
    protected void onResume() {
        super.onResume ( );
        new Thread ( new Runnable() {
            @Override
            public void run() {
                for(Parameters parameter : parameters){
                    runOnUiThread ( new Runnable() {
                        @Override
                        public void run() {
                            voltageSeries.appendData(new DataPoint( Double.parseDouble(parameter.unixTime),
                                    Double.parseDouble(parameter.voltage)),true,50);
                            currentSeries.appendData(new DataPoint( Double.parseDouble(parameter.unixTime),
                                    Double.parseDouble(parameter.current)),true,50);
                        }
                    } );
                }
                parameters.clear ();
            }
        } ).start ();
    }
    */
}