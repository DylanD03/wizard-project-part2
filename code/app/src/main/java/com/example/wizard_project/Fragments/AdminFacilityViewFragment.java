package com.example.wizard_project.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.wizard_project.Adapters.BrowseFacilityAdapter;
import com.example.wizard_project.Classes.Facility;
import com.example.wizard_project.Controllers.FacilityController;
import com.example.wizard_project.R;
import com.example.wizard_project.databinding.FragmentFacilityListBinding;

import java.util.ArrayList;

/**
 * AdminFacilityViewFragment displays a list of all facilities for the admin user,
 * allowing navigation to facility details.
 */
public class AdminFacilityViewFragment extends Fragment {
    private final ArrayList<Facility> facilityList = new ArrayList<>();
    private FragmentFacilityListBinding binding;
    private BrowseFacilityAdapter adapter;
    private FacilityController facilityController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFacilityListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the FacilityController and adapter
        facilityController = new FacilityController();
        adapter = new BrowseFacilityAdapter(getContext(), facilityList);

        // Set up the ListView and adapter
        ListView facilityListView = binding.facilityListview;
        facilityListView.setAdapter(adapter);

        // Load all facilities from the database
        loadFacilities();

        // Handle item clicks to navigate to event details
        binding.facilityListview.setOnItemClickListener((adapterView, itemView, position, id) -> {
            Facility selectedFacility = facilityList.get(position);
            navigateToFacilityDetails(selectedFacility);
        });
    }

    /**
     * Loads all facilities from Firestore and updates the facility list.
     */
    private void loadFacilities() {
        facilityController.getFacilities(facilities -> {
            facilityList.clear(); // Clear the current list to avoid duplicates
            facilityList.addAll(facilities); // Add new facilities
            adapter.notifyDataSetChanged(); // Notify the adapter to refresh the ListView
        });
    }

    /**
     * Navigates to the facility details fragment with the selected facility.
     *
     * @param selectedFacility The facility selected by the user.
     */
    private void navigateToFacilityDetails(Facility selectedFacility) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("facility", selectedFacility);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_AdminFragmentFacilityView_to_FacilityFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Clean up binding reference
    }
}
