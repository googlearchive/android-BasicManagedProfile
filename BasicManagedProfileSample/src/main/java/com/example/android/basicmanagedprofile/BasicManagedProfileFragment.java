/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.basicmanagedprofile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import static android.app.admin.DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT;
import static android.app.admin.DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED;

/**
 * Provides several functions that are available in a managed profile. This includes
 * enabling/disabling other apps, setting app restrictions, enabling/disabling intent forwarding,
 * and wiping out all the data in the profile.
 */
public class BasicManagedProfileFragment extends Fragment
    implements View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {

    /** Package names of calculator */
    private static final String[] PACKAGE_NAMES_CALCULATOR = {
            "com.android.calculator2"
    };

    /** Package names of Chrome */
    private static final String[] PACKAGE_NAMES_CHROME = {
            "com.android.chrome",
            "com.google.android.apps.chrome_dev",
            "com.chrome.canary",
            "com.chrome.beta",
    };

    private Button mButtonRemoveProfile;

    /** Whether the calculator app is enabled in this profile */
    private boolean mCalculatorEnabled;

    /** Whether Chrome is enabled in this profile */
    private boolean mChromeEnabled;

    public BasicManagedProfileFragment() {
    }

    public static BasicManagedProfileFragment newInstance() {
        return new BasicManagedProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Gets an instance of DevicePolicyManager
        DevicePolicyManager manager =
            (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        // Retrieves whether the calculator app is enabled in this profile
        mCalculatorEnabled = !manager.isApplicationHidden(
            BasicDeviceAdminReceiver.getComponentName(activity), PACKAGE_NAMES_CALCULATOR[0]);
        // Retrieves whether Chrome is enabled in this profile
        mChromeEnabled = false;
        for (String packageName : PACKAGE_NAMES_CHROME) {
            if (!manager.isApplicationHidden(
                    BasicDeviceAdminReceiver.getComponentName(activity), packageName)) {
                mChromeEnabled = true;
                return;
            }
       }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Bind event listeners and initial states
        view.findViewById(R.id.set_chrome_restrictions).setOnClickListener(this);
        view.findViewById(R.id.enable_forwarding).setOnClickListener(this);
        view.findViewById(R.id.disable_forwarding).setOnClickListener(this);
        view.findViewById(R.id.send_intent).setOnClickListener(this);
        mButtonRemoveProfile = (Button) view.findViewById(R.id.remove_profile);
        mButtonRemoveProfile.setOnClickListener(this);
        Switch toggleCalculator = (Switch) view.findViewById(R.id.toggle_calculator);
        toggleCalculator.setChecked(mCalculatorEnabled);
        toggleCalculator.setOnCheckedChangeListener(this);
        Switch toggleChrome = (Switch) view.findViewById(R.id.toggle_chrome);
        toggleChrome.setChecked(mChromeEnabled);
        toggleChrome.setOnCheckedChangeListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.set_chrome_restrictions: {
                setChromeRestrictions();
                break;
            }
            case R.id.enable_forwarding: {
                enableForwarding();
                break;
            }
            case R.id.disable_forwarding: {
                disableForwarding();
                break;
            }
            case R.id.send_intent: {
                sendIntent();
                break;
            }
            case R.id.remove_profile: {
                mButtonRemoveProfile.setEnabled(false);
                removeProfile();
                break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        switch (compoundButton.getId()) {
            case R.id.toggle_calculator: {
                setAppEnabled(PACKAGE_NAMES_CALCULATOR, checked);
                mCalculatorEnabled = checked;
                break;
            }
            case R.id.toggle_chrome: {
                setAppEnabled(PACKAGE_NAMES_CHROME, checked);
                mChromeEnabled = checked;
                break;
            }
        }
    }

    /**
     * Enables or disables the specified app in this profile.
     *
     * @param packageNames The package names of the target app.
     * @param enabled Pass true to enable the app.
     */
    private void setAppEnabled(String[] packageNames, boolean enabled) {
        Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        DevicePolicyManager manager =
            (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        for (String packageName : packageNames) {
            // This is how you can enable or disable an app in a managed profile.
            manager.setApplicationHidden(BasicDeviceAdminReceiver.getComponentName(activity),
                                          packageName, !enabled);
        }
        Toast.makeText(activity, enabled ? "Enabled" : "Disabled", Toast.LENGTH_SHORT).show();
    }

    /**
     * Sets restrictions to Chrome
     */
    private void setChromeRestrictions() {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        final DevicePolicyManager manager =
            (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final Bundle settings = new Bundle();
        settings.putString("EditBookmarksEnabled", "false");
        settings.putString("IncognitoModeAvailability", "1");
        settings.putString("ManagedBookmarks",
                           "[{\"name\": \"Chromium\", \"url\": \"http://chromium.org\"}, " +
                           "{\"name\": \"Google\", \"url\": \"https://www.google.com\"}]");
        settings.putString("DefaultSearchProviderEnabled", "true");
        settings.putString("DefaultSearchProviderName", "\"LMGTFY\"");
        settings.putString("DefaultSearchProviderSearchURL",
                "\"http://lmgtfy.com/?q={searchTerms}\"");
        settings.putString("URLBlacklist", "[\"example.com\", \"example.org\"]");
        StringBuilder message = new StringBuilder("Setting Chrome restrictions:");
        for (String key : settings.keySet()) {
            message.append("\n");
            message.append(key);
            message.append(": ");
            message.append(settings.getString(key));
        }
        ScrollView view = new ScrollView(activity);
        TextView text = new TextView(activity);
        text.setText(message);
        int size = (int) activity.getResources().getDimension(R.dimen.activity_horizontal_margin);
        view.setPadding(size, size, size, size);
        view.addView(text);
        new AlertDialog.Builder(activity)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        for (String packageName : PACKAGE_NAMES_CHROME) {
                            // This is how you can set restrictions to an app.
                            // The format for settings in Bundle differs from app to app.
                            manager.setApplicationRestrictions
                                    (BasicDeviceAdminReceiver.getComponentName(activity),
                                    packageName, settings);
                            Toast.makeText(activity, "Restrictions set.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    /**
     * Enables forwarding of share intent between private account and managed profile.
     */
    private void enableForwarding() {
        Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        DevicePolicyManager manager =
            (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SEND);
            filter.addDataType("text/plain");
            filter.addDataType("image/jpeg");
            // This is how you can register an IntentFilter as allowed pattern of Intent forwarding
            manager.addCrossProfileIntentFilter(BasicDeviceAdminReceiver.getComponentName(activity),
                                              filter,
                    FLAG_MANAGED_CAN_ACCESS_PARENT |  FLAG_PARENT_CAN_ACCESS_MANAGED );
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
    }

    /**
     * Disables forwarding of all intents.
     */
    private void disableForwarding() {
        Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        DevicePolicyManager manager =
            (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        manager.clearCrossProfileIntentFilters(BasicDeviceAdminReceiver.getComponentName(activity));
    }

    /**
     * Sends a sample intent.
     */
    private void sendIntent() {
        Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        DevicePolicyManager manager =
            (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,
                        manager.isProfileOwnerApp(activity.getApplicationContext().getPackageName())
                                ? "From the managed account" : "From the primary account");
        startActivity(intent);
    }

    /**
     * Wipes out all the data related to this managed profile.
     */
    private void removeProfile() {
        Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        DevicePolicyManager manager =
            (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        manager.wipeData(0);
        // The screen turns off here
    }

}
