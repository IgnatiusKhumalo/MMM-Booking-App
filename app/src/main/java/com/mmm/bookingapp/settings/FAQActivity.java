package com.mmm.bookingapp.settings;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mmm.bookingapp.R;
import com.mmm.bookingapp.adapters.FAQListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 17 - FAQ Screen. Reuses the practice's real, previously verified
 * FAQ content from their existing website, per the Blueprint.
 */
public class FAQActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView recyclerFAQ = findViewById(R.id.recyclerFAQ);
        recyclerFAQ.setLayoutManager(new LinearLayoutManager(this));

        List<FAQListAdapter.FaqItem> faqItems = new ArrayList<>();
        faqItems.add(new FAQListAdapter.FaqItem(
                "What services do you offer?",
                "We provide counselling, trauma support, family therapy, couples counselling, employee wellness programmes, psychological support, occupational therapy, and community development services."
        ));
        faqItems.add(new FAQListAdapter.FaqItem(
                "How do I book an appointment?",
                "You can book directly through this app, WhatsApp us, call us, or send us an email."
        ));
        faqItems.add(new FAQListAdapter.FaqItem(
                "Do you offer online consultations?",
                "Yes. We offer both online and in-person consultations depending on your needs."
        ));
        faqItems.add(new FAQListAdapter.FaqItem(
                "Is my information confidential?",
                "Absolutely. Every consultation is handled professionally and confidentially according to ethical guidelines."
        ));
        faqItems.add(new FAQListAdapter.FaqItem(
                "Where are you located?",
                "We are based in Midrand, Johannesburg, Centurion and surrounding areas around Gauteng, and also provide selected online services."
        ));

        recyclerFAQ.setAdapter(new FAQListAdapter(faqItems));
    }
}