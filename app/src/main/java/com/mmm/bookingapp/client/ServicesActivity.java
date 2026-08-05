package com.mmm.bookingapp.client;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.adapters.ServiceListAdapter;

import java.util.Arrays;
import java.util.List;

/**
 * Screen 7 - Services Screen.
 * Displays the practice's 7 real services (Blueprint, Section 3),
 * informational only - no click action, no navigation from this screen.
 */
public class ServicesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView recyclerServices = findViewById(R.id.recyclerServices);
        recyclerServices.setLayoutManager(new LinearLayoutManager(this));

        String[] servicesArray = getResources().getStringArray(R.array.services_list);
        List<String> services = Arrays.asList(servicesArray);

        recyclerServices.setAdapter(new ServiceListAdapter(services));
    }
}