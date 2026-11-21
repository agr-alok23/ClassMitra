package com.classmitra.app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.classmitra.app.databinding.ItemRecentUploadBinding;
import com.classmitra.app.models.PdfUpload;

import java.util.List;

/**
 * Adapter for showing recent PDF uploads in the dashboard.
 */
public class PdfUploadAdapter extends RecyclerView.Adapter<PdfUploadAdapter.PdfViewHolder> {

    private final List<PdfUpload> pdfList;

    public PdfUploadAdapter(List<PdfUpload> pdfList) {
        this.pdfList = pdfList;
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PdfViewHolder(
                ItemRecentUploadBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        holder.bind(pdfList.get(position));
    }

    @Override
    public int getItemCount() {
        return pdfList.size();
    }

    static class PdfViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecentUploadBinding binding;

        public PdfViewHolder(ItemRecentUploadBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PdfUpload pdf) {
            binding.tvFileName.setText(pdf.getFileName());
            binding.tvUploadDate.setText(pdf.getUploadDate());
        }
    }
}