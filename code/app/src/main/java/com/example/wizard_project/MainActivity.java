package com.example.wizard_project;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.wizard_project.Classes.PhotoHandler;
import com.example.wizard_project.Classes.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.example.wizard_project.databinding.ActivityMainBinding;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * MainActivity is the central activity for the EventWizard app, managing user authentication,
 * navigation between fragments, and Firestore data storage. This class also handles user-specific
 * tasks, including retrieving and storing profile data and configuring the UI based on user roles.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private User currentUser;
    private User deleteUser;
    private PhotoHandler photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up view binding and content view
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // TODO: Delete when done testing
        //addSampleUsersToDatabase();

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // TODO: Delete when done testing
        // ImageView image = findViewById(R.id.event_wizard_logo);
        // photo = new PhotoHandler();
        // photo.loadImage("IMG_0113.JPG",image,this);
        // photo.getUserImage(this);

        // Initialize navigation components
        setupNavigation();

        // Retrieve the device ID, then initialize the user
        String deviceId = retrieveDeviceId();
        initializeUser(deviceId, () -> {
            if (currentUser != null) {
                Toast.makeText(this, "Welcome to EventWizard! ", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "User data not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Configures bottom navigation and toolbar visibility for specific fragments.
     */
    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Connect NavController to BottomNavigationView
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Handle bottom navigation item selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Home Button
            if (item.getItemId() == R.id.nav_home) {
                navController.navigate(R.id.HomeFragment);
                return true;
            }
            // TODO: Camera Button
            // TODO: Menu Button
            return false;
        });

        // Toggle toolbar visibility based on the current fragment
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // Entrant toolbar
            if (destination.getId() == R.id.EntrantFragment) {
                binding.entrantToolbar.setVisibility(View.VISIBLE);
            } else {
                binding.entrantToolbar.setVisibility(View.GONE);
            }
            // TODO: Create custom toolbars for other fragments
        });
    }

    /**
     * Retrieves the unique device ID from the user's device.
     *
     * @return A string representing the device ID.
     */
    public String retrieveDeviceId() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Initializes the user by fetching or creating the user document in Firestore.
     *
     * @param deviceId The unique device ID used as the document ID in Firestore.
     * @param onUserInitialized A callback to indicate when the user is initialized.
     */
    private void initializeUser(String deviceId, Runnable onUserInitialized) {
        db.collection("users").document(deviceId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                // If the document exists, load data into currentUser
                if (document.exists()) {
                    currentUser = new User();
                    currentUser.setUserData(document);
                // If the document does not exist, create a new user
                } else {
                    currentUser = new User(deviceId, "", "", false, false, false, "", "", "","");
                    Map<String, Object> userData = createUserDataMap(currentUser);
                    db.collection("users").document(deviceId).set(userData)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "New user created", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> {
                                currentUser = null;
                                Toast.makeText(this, "Failed to create user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            // Error handling (unable to fetch document)
            } else {
                currentUser = null;
                Toast.makeText(this, "Error fetching user data: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
            }
            onUserInitialized.run(); // Notify that user data is initialized (success or failure)
        });
    }

    /**
     * Creates a map of user data to be stored in Firestore.
     *
     * @param user The user object containing data to be stored.
     * @return A map of user data.
     */
    private Map<String, Object> createUserDataMap(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("deviceId", user.getDeviceId());
        userData.put("email", user.getEmail());
        userData.put("location", user.getLocation());
        userData.put("IsAdmin", user.isAdmin());
        userData.put("IsEntrant", user.isEntrant());
        userData.put("isOrganizer", user.isOrganizer());
        userData.put("name", user.getName());
        userData.put("phoneNumber", user.getPhoneNumber());
        userData.put("photoId", user.getProfilePictureUri());
        userData.put("profilePath", user.getProfilePath());
        return userData;
    }

    /**
     * Loads the current user's data from Firestore into the User object.
     * TODO: Delete if no longer needed (currently not used)
     *
     * @param deviceId The unique device ID used as the document ID in Firestore.
     * @param callback The callback to notify when the user is loaded.
     */
    private void loadCurrentUser(String deviceId, UserLoadCallback callback) {
        // Retrieve the user document from Firestore
        db.collection("users").document(deviceId).get().addOnSuccessListener(documentSnapshot -> {
            // Create and populate a new User object with the retrieved data
            if (documentSnapshot.exists()) {
                currentUser = new User();
                currentUser.setUserData(documentSnapshot);
                callback.onUserLoaded(currentUser);
            // If the user document does not exist, set currentUser to null
            } else {
                currentUser = null;
                callback.onUserLoaded(null);
            }
        // If there was an error retrieving the user document, set currentUser to null
        }).addOnFailureListener(e -> {
            currentUser = null;
            callback.onUserLoaded(null);
        });
    }

    /**
     * Returns the current user's data.
     *
     * @return The current User object or null if the user is not loaded.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the user to delete.
     * Used for admin functionality.
     *
     * @param newUser The user to mark for deletion.
     */
    public void setDeleteUser(User newUser) {
        deleteUser = newUser;
    }

    /**
     * Retrieves the user marked for deletion.
     * Used for admin functionality.
     *
     * @return The User object marked for deletion.
     */
    public User getDeleteUser() {
        return deleteUser;
    }

    // TODO: Delete when done testing
    public void addSampleUsersToDatabase() {
        for (int i = 1; i <= 10; i++) {
            // Create a sample User object
            String id =Integer.toString(i);
            User sampleUsers =  new User(id, "@gmail.com", "58888 north ave", false, false, false, "Jerry ", "213123123123", "","");
            Map<String, Object> userData = createUserDataMap(sampleUsers);
            db.collection("users").document(id).set(userData)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "New user created", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        currentUser = null;
                        Toast.makeText(this, "Failed to create user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Interface for a callback when the current user is loaded.
     */
    public interface UserLoadCallback {
        void onUserLoaded(User user);
    }

    /**
     * Retrieves the Firebase Storage reference.
     *
     * @return The root Firebase Storage reference.
     */
    public StorageReference getStorageRef() {
        return storageRef;
    }
}
