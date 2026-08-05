package com.mmm.bookingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mmm.bookingapp.R;

import java.util.List;

/**
 * RecyclerView adapter for the FAQ screen (Screen 17), per the Project
 * Structure document. Each row expands/collapses its answer on tap.
 */
public class FAQListAdapter extends RecyclerView.Adapter<FAQListAdapter.FAQViewHolder> {

    public static class FaqItem {
        public final String question;
        public final String answer;
        public boolean expanded = false;

        public FaqItem(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    private final List<FaqItem> faqItems;

    public FAQListAdapter(List<FaqItem> faqItems) {
        this.faqItems = faqItems;
    }

    @NonNull
    @Override
    public FAQViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_faq_card, parent, false);
        return new FAQViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FAQViewHolder holder, int position) {
        FaqItem item = faqItems.get(position);
        holder.tvQuestion.setText(item.question);
        holder.tvAnswer.setText(item.answer);
        holder.tvAnswer.setVisibility(item.expanded ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            item.expanded = !item.expanded;
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return faqItems.size();
    }

    static class FAQViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvAnswer;

        FAQViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
        }
    }
}