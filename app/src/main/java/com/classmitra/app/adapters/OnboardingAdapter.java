package com.classmitra.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.classmitra.app.databinding.ItemOnboardingBinding;
import com.classmitra.app.models.OnboardingItem;

import java.util.List;

/**
 * Adapter for ViewPager2 in OnboardingActivity.
 */
public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private final List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OnboardingViewHolder(
                ItemOnboardingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private final ItemOnboardingBinding binding;

        public OnboardingViewHolder(ItemOnboardingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OnboardingItem item) {
            binding.tvTitle.setText(item.getTitle());
            binding.tvDescription.setText(item.getDescription());
            binding.ivOnboardingImage.setImageResource(item.getImageRes());

            if (item.getSubtitle() != null && !item.getSubtitle().isEmpty()) {
                binding.tvSubtitle.setText(item.getSubtitle());
                binding.tvSubtitle.setVisibility(View.VISIBLE);
            } else {
                binding.tvSubtitle.setVisibility(View.GONE);
            }
        }
    }
}