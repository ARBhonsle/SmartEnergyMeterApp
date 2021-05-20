package com.example.smartenergymeterapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class DisplayReadingActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var update: Button
    private lateinit var download: Button
    private lateinit var home: Button
    private lateinit var query: Query
    var key: String? = null
    private lateinit var data : Parameters

    class Parameters {
        var voltage: Double? = null
        var current: Double? = null
        var frequency: Double? = null
        constructor() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        constructor(username: Double?, email: Double?, frequency: Double) {
            this.voltage = username
            this.current = email
            this.frequency = frequency
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_reading)
        database = FirebaseDatabase.getInstance().reference.child("time")
        update = findViewById(R.id.update)
        update.setOnClickListener{
            query = database.limitToLast(1)
            query.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (child in dataSnapshot.children) {
                        key = child.key
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {}
            })
            //var ref = FirebaseDatabase.getInstance().getReference(key)
            data = Parameters()
        }
        download = findViewById(R.id.download)
        download.setOnClickListener{
        }
        home =findViewById(R.id.back)
        home.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}