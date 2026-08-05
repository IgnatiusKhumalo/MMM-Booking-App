package com.mmm.bookingapp.client;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.models.PracticeInfo;
import com.mmm.bookingapp.utils.FirebaseRefs;

import java.util.List;

/**
 * Screen 11 - Contact / Help Screen.
 * Reads practiceInfo/main from Firestore and displays it, with tap-to-call,
 * tap-to-email, and tap-to-WhatsApp actions via standard Android Intents,
 * per the Blueprint's Screen 11 description. Falls back to a hardcoded
 * PracticeInfo if the document is ever missing or fails to load.
 */
public class ContactActivity extends AppCompatActivity {

    private TextView tvCrisisLine, tvPhone, tvEmail, tvWhatsApp, tvLocations, tvHours;
    private PracticeInfo currentInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvCrisisLine = findViewById(R.id.tvCrisisLine);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvWhatsApp = findViewById(R.id.tvWhatsApp);
        tvLocations = findViewById(R.id.tvLocations);
        tvHours = findViewById(R.id.tvHours);

        loadPracticeInfo();

        findViewById(R.id.rowCallUs).setOnClickListener(v -> callPractice());
        findViewById(R.id.rowEmailUs).setOnClickListener(v -> emailPractice());
        findViewById(R.id.rowWhatsApp).setOnClickListener(v -> whatsAppPractice());
    }

    private void loadPracticeInfo() {
        FirebaseRefs.practiceInfoDoc().get()
                .addOnSuccessListener(document -> {
                    PracticeInfo info = document.exists()
                            ? document.toObject(PracticeInfo.class)
                            : PracticeInfo.getFallback();
                    displayInfo(info != null ? info : PracticeInfo.getFallback());
                })
                .addOnFailureListener(e -> displayInfo(PracticeInfo.getFallback()));
    }

    private void displayInfo(PracticeInfo info) {
        currentInfo = info;

        tvCrisisLine.setText(info.getCrisisLineNote());
        tvPhone.setText(info.getPhone());
        tvEmail.setText(info.getEmail());

        List<String> locations = info.getLocations();
        if (locations != null) {
            tvLocations.setText(String.join(", ", locations));
        }

        if (info.getOperatingHours() != null) {
            String hoursText = info.getOperatingDays() + "  ·  "
                    + info.getOperatingHours().getStart() + " - " + info.getOperatingHours().getEnd();
            tvHours.setText(hoursText);
        } else {
            tvHours.setText(info.getOperatingDays());
        }
    }

    private void callPractice() {
        if (currentInfo == null) return;
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentInfo.getPhone()));
        startActivity(intent);
    }

    private void emailPractice() {
        if (currentInfo == null) return;
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + currentInfo.getEmail()));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.contact_no_email_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void whatsAppPractice() {
        if (currentInfo == null || currentInfo.getWhatsappLink() == null) return;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentInfo.getWhatsappLink()));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.contact_no_whatsapp, Toast.LENGTH_SHORT).show();
        }
    }
}