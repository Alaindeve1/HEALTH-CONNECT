
package rw.ac.auca.finalprojectgroupfa.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import rw.ac.auca.finalprojectgroupfa.fragments.ActiveConsultationsFragment;
import rw.ac.auca.finalprojectgroupfa.fragments.PendingConsultationsFragment;

public class ConsultationsPagerAdapter extends FragmentStateAdapter {

    public ConsultationsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a NEW fragment instance for the given position
        switch (position) {
            case 0:
                return new PendingConsultationsFragment();
            case 1:
                return new ActiveConsultationsFragment();
            default:
                // This should never happen, but it's good practice to have a default
                return new PendingConsultationsFragment();
        }
    }

    @Override
    public int getItemCount() {
        // We have two tabs
        return 2;
    }
}
