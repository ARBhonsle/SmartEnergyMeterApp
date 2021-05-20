package com.example.smartenergymeterapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var logoutBtn: Button
    private lateinit var updatePass: Button
    private lateinit var viewGraph: Button
    private lateinit var readDisp: Button
    private lateinit var bluetoothdata : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        if(auth.currentUser == null){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }else{
            Toast.makeText(this, "Already logged in", Toast.LENGTH_LONG).show()
        }

        setContentView(R.layout.activity_main)

        logoutBtn = findViewById(R.id.logout_btn)
        updatePass = findViewById(R.id.update_pass_btn)
        viewGraph = findViewById(R.id.view_graph)
        readDisp=findViewById(R.id.readingDisplay)
        bluetoothdata=findViewById(R.id.bluetooth)

        logoutBtn.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        updatePass.setOnClickListener{
            val intent = Intent(this, UpdatePassword::class.java)
            startActivity(intent)
        }
        viewGraph.setOnClickListener{
            val intent = Intent(this, ViewGraphActivity::class.java)
            startActivity(intent)
        }
        readDisp.setOnClickListener{
            val intent = Intent(this, DisplayReadingActivity::class.java)
            startActivity(intent)
        }
        bluetoothdata.setOnClickListener{
            val intent = Intent(this, BluetoothMainActivity::class.java)
            startActivity(intent)
        }

    }
}