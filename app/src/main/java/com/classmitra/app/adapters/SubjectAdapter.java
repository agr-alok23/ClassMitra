package com.classmitra.app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.classmitra.app.databinding.ItemSubjectBinding;
import com.classmitra.app.models.Subject;

import java.util.List;

/**
 * Adapter for academic subjects list.
 */
public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {

    private final List<Subject> subjects;
    private final OnSubjectClickListener listener;

    public interface OnSubjectClickListener {
        void onSubjectClick(Subject subject);
    }

    public SubjectAdapter(List<Subject> subjects, OnSubjectClickListener listener) {
        this.subjects = subjects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SubjectViewHolder(
                ItemSubjectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        holder.bind(subjects.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class SubjectViewHolder extends RecyclerView.ViewHolder {
        private final ItemSubjectBinding binding;

        public SubjectViewHolder(ItemSubjectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Subject subject, OnSubjectClickListener listener) {
            binding.tvSubjectName.setText(subject.getName());
            binding.tvNotesCount.setText(subject.getNotesCount() + " Notes");
            binding.tvQuizCount.setText(subject.getQuizCount() + " Quizzes");
            binding.ivSubjectIcon.setColorFilter(subject.getColor());
            
            binding.getRoot().setOnClickListener(v -> listener.onSubjectClick(subject));
        }
    }
}