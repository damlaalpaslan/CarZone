package com.ece.aractakip;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.ece.aractakip.adapter.ReminderSectionAdapter;
import com.ece.aractakip.adapter.ServiceHistoryAdapter;
import com.ece.aractakip.data.AnomalyNotificationRepository;
import com.ece.aractakip.data.AppPreferences;
import com.ece.aractakip.data.ReminderRepository;
import com.ece.aractakip.databinding.ActivityMainBinding;
import com.ece.aractakip.model.Reminder;
import com.ece.aractakip.model.ServiceHistoryEntry;
import com.ece.aractakip.model.VehicleModel;
import com.ece.aractakip.ui.AnomalyNotificationsBottomSheet;
import com.ece.aractakip.ui.DocumentsFragment;
import com.ece.aractakip.ui.ExpenseAnalysisFragment;
import com.ece.aractakip.ui.NearServicesFragment;
import com.ece.aractakip.ui.RemindersFragment;
import com.ece.aractakip.ui.ServiceHistoryFragment;
import com.ece.aractakip.ui.SettingsFragment;
import com.ece.aractakip.ui.StatisticsFragment;
import com.ece.aractakip.util.MileageFormatUtil;
import com.ece.aractakip.util.ThemeHelper;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.appbar.AppBarLayout;
import com.onesignal.OneSignal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        ServiceHistoryFragment.ServiceHistoryHost,
        DocumentsFragment.DocumentsHost,
        NearServicesFragment.NearServicesHost,
        ExpenseAnalysisFragment.ExpenseAnalysisHost {

    private static final String STATE_SELECTED_NAV = "state_selected_nav";
    private static final String STATE_SHOWING_SERVICE_HISTORY = "state_showing_service_history";
    private static final String STATE_SHOWING_NEAR_SERVICES = "state_showing_near_services";
    private static final String STATE_SHOWING_EXPENSE_ANALYSIS = "state_showing_expense_analysis";

    private static final String STATE_SHOWING_DOCUMENTS = "state_showing_documents";

    private ActivityMainBinding binding;
    private int selectedNavId = R.id.nav_home;
    private boolean showingServiceHistory;
    private boolean showingDocuments;
    private boolean showingNearServices;
    private boolean showingExpenseAnalysis;
    @Nullable
    private BadgeDrawable notificationBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);
        OneSignal.setAppId("f680a6e4-07ac-4686-b655-d2603e0d1dcc");
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("anomali_bildirimleri")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("FCM", "Anomali kanalına başarıyla abone olundu!");
                    }
                });

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            applyMainInsets(systemBars);
            return insets;
        });

        if (!AppPreferences.isOnboardingCompleted(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        if (savedInstanceState != null) {
            selectedNavId = savedInstanceState.getInt(STATE_SELECTED_NAV, R.id.nav_home);
            showingServiceHistory = savedInstanceState.getBoolean(STATE_SHOWING_SERVICE_HISTORY, false);
            showingDocuments = savedInstanceState.getBoolean(STATE_SHOWING_DOCUMENTS, false);
            showingNearServices = savedInstanceState.getBoolean(STATE_SHOWING_NEAR_SERVICES, false);
            showingExpenseAnalysis = savedInstanceState.getBoolean(STATE_SHOWING_EXPENSE_ANALYSIS, false);
        }

        setSupportActionBar(binding.toolbar);

        binding.buttonNotifications.setOnClickListener(v -> showAnomalyNotificationsSheet());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showHome();
                selectedNavId = R.id.nav_home;
                return true;
            } else if (id == R.id.nav_stats) {
                showStats();
                selectedNavId = R.id.nav_stats;
                return true;
            } else if (id == R.id.nav_reminders) {
                showReminders();
                selectedNavId = R.id.nav_reminders;
                return true;
            } else if (id == R.id.nav_settings) {
                showSettings();
                selectedNavId = R.id.nav_settings;
                return true;
            }
            return false;
        });

        setupNotificationBadge();

        binding.cardQuickService.setOnClickListener(v -> showServiceHistory());
        binding.cardQuickDocuments.setOnClickListener(v -> showDocuments());
        binding.cardQuickNearby.setOnClickListener(v -> showNearServices());
        binding.cardQuickExpense.setOnClickListener(v -> showExpenseAnalysis());
        binding.textAlertsSeeAll.setOnClickListener(v -> openRemindersTab());

        VehicleModel vehicle = AppPreferences.loadVehicle(this);
        bindVehicle(vehicle);
        bindRecentServices();
        bindUpcomingAlerts();
        binding.bottomNavigation.setSelectedItemId(selectedNavId);
        if (showingServiceHistory) {
            showServiceHistory();
        } else if (showingDocuments) {
            showDocuments();
        } else if (showingNearServices) {
            showNearServices();
        } else if (showingExpenseAnalysis) {
            showExpenseAnalysis();
        } else {
            navigateToTab(selectedNavId);
        }
        yerelAnomaliDinleyicisiBaslat();
    }



    private void setupNotificationBadge() {
        binding.notificationBellContainer.post(() -> {
            if (notificationBadge != null) {
                updateNotificationBadge();
                return;
            }
            notificationBadge = BadgeDrawable.create(this);
            notificationBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.service_accent_red));
            notificationBadge.setBadgeGravity(BadgeDrawable.TOP_END);
            notificationBadge.setVisible(false);
            BadgeUtils.attachBadgeDrawable(notificationBadge, binding.buttonNotifications);
            updateNotificationBadge();
        });
    }

    private void updateNotificationBadge() {
        if (notificationBadge == null) {
            return;
        }
        boolean hasUnread = AnomalyNotificationRepository.getUnreadCount(this) > 0;
        notificationBadge.clearNumber();
        notificationBadge.setVisible(hasUnread);
    }

    private void showAnomalyNotificationsSheet() {
        AnomalyNotificationsBottomSheet sheet = AnomalyNotificationsBottomSheet.newInstance();
        sheet.setListener(this::updateNotificationBadge);
        sheet.show(getSupportFragmentManager(), "anomaly_notifications");
    }

    private void onAnomalyReceived(@NonNull String message) {
        AnomalyNotificationRepository.add(this, message);
        bildirimiEkranaFirlat(getString(R.string.anomaly_default_title), message);
        updateNotificationBadge();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_NAV, selectedNavId);
        outState.putBoolean(STATE_SHOWING_SERVICE_HISTORY, showingServiceHistory);
        outState.putBoolean(STATE_SHOWING_DOCUMENTS, showingDocuments);
        outState.putBoolean(STATE_SHOWING_NEAR_SERVICES, showingNearServices);
        outState.putBoolean(STATE_SHOWING_EXPENSE_ANALYSIS, showingExpenseAnalysis);
    }

    private void navigateToTab(int navId) {
        if (navId == R.id.nav_stats) {
            showStats();
        } else if (navId == R.id.nav_reminders) {
            showReminders();
        } else if (navId == R.id.nav_settings) {
            showSettings();
        } else {
            showHome();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VehicleModel vehicle = AppPreferences.loadVehicle(this);
        bindVehicle(vehicle);
        bindRecentServices();
        bindUpcomingAlerts();
        if (showingServiceHistory) {
            binding.appBarLayout.setVisibility(View.GONE);
            binding.scrollContent.setVisibility(View.GONE);
            binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        } else if (showingDocuments) {
            binding.appBarLayout.setVisibility(View.GONE);
            binding.scrollContent.setVisibility(View.GONE);
            binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        } else if (showingNearServices) {
            setStatusBarForNearServices(true);
            binding.appBarLayout.setVisibility(View.GONE);
            binding.scrollContent.setVisibility(View.GONE);
            binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        } else if (showingExpenseAnalysis) {
            binding.appBarLayout.setVisibility(View.GONE);
            binding.scrollContent.setVisibility(View.GONE);
            binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        } else if (selectedNavId == R.id.nav_reminders
                || selectedNavId == R.id.nav_stats
                || selectedNavId == R.id.nav_settings) {
            binding.appBarLayout.setVisibility(View.VISIBLE);
            binding.scrollContent.setVisibility(View.GONE);
            binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        } else {
            binding.appBarLayout.setVisibility(View.VISIBLE);
            binding.scrollContent.setVisibility(View.VISIBLE);
            binding.remindersFragmentContainer.setVisibility(View.GONE);
        }
        updateNotificationBadge();
    }

    private void bindVehicle(@NonNull VehicleModel v) {
        binding.textVehicleTitle.setText((v.getBrand() + " " + v.getModel()).trim());
        binding.textVehiclePlate.setText(v.getPlate());

        if (v.getMileage() > 0) {
            binding.textVehicleMileage.setText(MileageFormatUtil.formatKm(v.getMileage()));
            binding.rowMileage.setVisibility(View.VISIBLE);
        } else {
            binding.rowMileage.setVisibility(View.GONE);
        }

        String photoUri = v.getPhotoUri();
        if (photoUri != null && !photoUri.isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(photoUri))
                    .centerCrop()
                    .placeholder(R.drawable.setup_ic_vehicle_placeholder)
                    .into(binding.imageVehicle);
        } else {
            Glide.with(this)
                    .load(R.drawable.setup_ic_vehicle_placeholder)
                    .centerCrop()
                    .into(binding.imageVehicle);
        }
    }

    private void bindRecentServices() {
        List<ServiceHistoryEntry> entries = new ArrayList<>(AppPreferences.getServiceHistory(this));
        entries.sort(Comparator.comparingLong(ServiceHistoryEntry::getServiceDateMs).reversed());

        binding.containerRecentServices.removeAllViews();
        if (entries.isEmpty()) {
            binding.containerRecentServices.setVisibility(View.GONE);
            binding.cardRecentServicesEmpty.setVisibility(View.VISIBLE);
            binding.cardRecentServicesEmpty.setOnClickListener(v -> showServiceHistory());
            return;
        }

        binding.cardRecentServicesEmpty.setVisibility(View.GONE);
        binding.containerRecentServices.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        int marginTopPx = (int) (8 * getResources().getDisplayMetrics().density);
        int count = Math.min(2, entries.size());
        for (int i = 0; i < count; i++) {
            View itemView = inflater.inflate(
                    R.layout.item_dashboard_recent_service,
                    binding.containerRecentServices,
                    false);
            bindRecentServiceItem(itemView, entries.get(i));
            itemView.setOnClickListener(v -> showServiceHistory());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                params.topMargin = marginTopPx;
            }
            binding.containerRecentServices.addView(itemView, params);
        }
    }

    private void bindRecentServiceItem(@NonNull View itemView, @NonNull ServiceHistoryEntry entry) {
        TextView titleView = itemView.findViewById(R.id.textRecentServiceTitle);
        TextView metaView = itemView.findViewById(R.id.textRecentServiceMeta);
        FrameLayout iconFrame = itemView.findViewById(R.id.frameRecentServiceIcon);
        ImageView iconView = itemView.findViewById(R.id.imageRecentServiceIcon);

        titleView.setText(entry.getTitle());
        metaView.setText(getString(
                R.string.recent_service_meta_template,
                entry.getDateText(),
                ServiceHistoryAdapter.formatCost(entry.getCost()),
                MileageFormatUtil.formatKm(entry.getMileage())));

        iconFrame.setBackgroundResource(resolveRecentServiceIconBackground(entry.getType()));
        iconView.setImageResource(resolveRecentServiceIcon(entry.getType()));
        if (entry.getType() == ServiceHistoryEntry.Type.HEAVY_MAINTENANCE) {
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.service_accent_orange));
        } else {
            iconView.clearColorFilter();
        }
    }

    private static int resolveRecentServiceIcon(@NonNull ServiceHistoryEntry.Type type) {
        if (type == ServiceHistoryEntry.Type.REPAIR) {
            return R.drawable.ic_service_brake;
        }
        if (type == ServiceHistoryEntry.Type.HEAVY_MAINTENANCE) {
            return R.drawable.ic_quick_wrench;
        }
        return R.drawable.ic_service_oil;
    }

    private static int resolveRecentServiceIconBackground(@NonNull ServiceHistoryEntry.Type type) {
        if (type == ServiceHistoryEntry.Type.REPAIR) {
            return R.drawable.bg_service_icon_red;
        }
        if (type == ServiceHistoryEntry.Type.HEAVY_MAINTENANCE) {
            return R.drawable.bg_service_icon_orange;
        }
        return R.drawable.bg_service_icon_blue;
    }

    private void showHome() {
        showingServiceHistory = false;
        showingDocuments = false;
        showingNearServices = false;
        showingExpenseAnalysis = false;
        setStatusBarForNearServices(false);
        applyFragmentHostEdgeToEdge(false);
        binding.appBarLayout.setVisibility(View.VISIBLE);
        binding.scrollContent.setVisibility(View.VISIBLE);
        binding.remindersFragmentContainer.setVisibility(View.GONE);
        bindVehicle(AppPreferences.loadVehicle(this));
        bindRecentServices();
        bindUpcomingAlerts();
        ViewCompat.requestApplyInsets(binding.main);
    }

    private void bindUpcomingAlerts() {
        List<Reminder> reminders = ReminderRepository.loadReminders(this);
        List<ReminderRepository.DashboardAlert> alerts =
                ReminderRepository.getUpcomingDateAlerts(this, reminders);

        binding.containerUpcomingAlerts.removeAllViews();
        if (alerts.isEmpty()) {
            binding.containerUpcomingAlerts.setVisibility(View.GONE);
            binding.cardUpcomingAlertsEmpty.setVisibility(View.VISIBLE);
            binding.cardUpcomingAlertsEmpty.setOnClickListener(v -> openRemindersTab());
            return;
        }

        binding.cardUpcomingAlertsEmpty.setVisibility(View.GONE);
        binding.containerUpcomingAlerts.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        int marginTopPx = (int) (8 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < alerts.size(); i++) {
            ReminderRepository.DashboardAlert alert = alerts.get(i);
            View itemView = inflater.inflate(
                    R.layout.item_dashboard_upcoming_alert,
                    binding.containerUpcomingAlerts,
                    false);
            bindUpcomingAlertItem(itemView, alert);
            itemView.setOnClickListener(v -> openRemindersTab());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                params.topMargin = marginTopPx;
            }
            binding.containerUpcomingAlerts.addView(itemView, params);
        }
    }

    private void bindUpcomingAlertItem(@NonNull View itemView,
                                       @NonNull ReminderRepository.DashboardAlert alert) {
        TextView titleView = itemView.findViewById(R.id.textAlertTitle);
        TextView countdownView = itemView.findViewById(R.id.textAlertCountdown);
        View indicator = itemView.findViewById(R.id.viewAlertCategoryIndicator);

        titleView.setText(alert.title);
        countdownView.setText(alert.countdownText);
        int indicatorColor = ReminderSectionAdapter.resolveCategoryColor(this, alert.category);
        if (alert.overdue) {
            indicatorColor = ContextCompat.getColor(this, R.color.service_accent_red);
            countdownView.setTextColor(ContextCompat.getColor(this, R.color.service_accent_red));
        } else {
            countdownView.setTextColor(ContextCompat.getColor(this, R.color.setup_primary_dark));
        }
        indicator.setBackgroundColor(indicatorColor);
    }

    private void openRemindersTab() {
        selectedNavId = R.id.nav_reminders;
        binding.bottomNavigation.setSelectedItemId(R.id.nav_reminders);
        showReminders();
    }

    private void showServiceHistory() {
        showingServiceHistory = true;
        showingDocuments = false;
        showingNearServices = false;
        showingExpenseAnalysis = false;
        selectedNavId = R.id.nav_home;
        setStatusBarForNearServices(false);
        applyFragmentHostEdgeToEdge(true);
        binding.appBarLayout.setVisibility(View.GONE);
        binding.scrollContent.setVisibility(View.GONE);
        binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        ViewCompat.requestApplyInsets(binding.main);
        replaceFragment(ServiceHistoryFragment.newInstance());
    }

    private void showDocuments() {
        showingServiceHistory = false;
        showingDocuments = true;
        showingNearServices = false;
        showingExpenseAnalysis = false;
        selectedNavId = R.id.nav_home;
        setStatusBarForNearServices(false);
        applyFragmentHostEdgeToEdge(true);
        binding.appBarLayout.setVisibility(View.GONE);
        binding.scrollContent.setVisibility(View.GONE);
        binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        ViewCompat.requestApplyInsets(binding.main);
        replaceFragment(DocumentsFragment.newInstance());
    }

    private void showNearServices() {
        showingServiceHistory = false;
        showingDocuments = false;
        showingNearServices = true;
        showingExpenseAnalysis = false;
        selectedNavId = R.id.nav_home;
        setStatusBarForNearServices(true);
        applyFragmentHostEdgeToEdge(true);
        binding.appBarLayout.setVisibility(View.GONE);
        binding.scrollContent.setVisibility(View.GONE);
        binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        ViewCompat.requestApplyInsets(binding.main);
        replaceFragment(NearServicesFragment.newInstance());
    }

    private void showExpenseAnalysis() {
        showingServiceHistory = false;
        showingDocuments = false;
        showingNearServices = false;
        showingExpenseAnalysis = true;
        selectedNavId = R.id.nav_home;
        setStatusBarForNearServices(false);
        applyFragmentHostEdgeToEdge(true);
        binding.appBarLayout.setVisibility(View.GONE);
        binding.scrollContent.setVisibility(View.GONE);
        binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        ViewCompat.requestApplyInsets(binding.main);
        replaceFragment(ExpenseAnalysisFragment.newInstance());
    }

    private void applyMainInsets(@NonNull Insets systemBars) {
        boolean fullScreenFragment = showingServiceHistory || showingDocuments
                || showingNearServices || showingExpenseAnalysis;
        int topInset = fullScreenFragment ? 0 : systemBars.top;
        binding.main.setPadding(systemBars.left, topInset, systemBars.right, systemBars.bottom);
        binding.notificationBellContainer.setPaddingRelative(
                8,
                0,
                16 + systemBars.right,
                0
        );
    }

    private void applyFragmentHostEdgeToEdge(boolean edgeToEdge) {
        ViewGroup.LayoutParams rawParams = binding.remindersFragmentContainer.getLayoutParams();
        if (rawParams instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) rawParams;
            if (edgeToEdge) {
                params.setBehavior(null);
                params.topMargin = 0;
            } else {
                params.setBehavior(new AppBarLayout.ScrollingViewBehavior());
            }
            binding.remindersFragmentContainer.setLayoutParams(params);
        }
        binding.remindersFragmentContainer.setBackgroundColor(edgeToEdge
                ? android.graphics.Color.TRANSPARENT
                : ContextCompat.getColor(this, R.color.setup_background));
        if (edgeToEdge) {
            binding.main.setPadding(
                    binding.main.getPaddingLeft(),
                    0,
                    binding.main.getPaddingRight(),
                    binding.main.getPaddingBottom());
        }
        ViewCompat.requestApplyInsets(binding.main);
    }

    private void setStatusBarForNearServices(boolean nearServices) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), !nearServices);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (nearServices) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.near_services_header));
            controller.setAppearanceLightStatusBars(false);
        } else {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.setup_background));
            controller.setAppearanceLightStatusBars(true);
        }
    }

    private void showReminders() {
        showingServiceHistory = false;
        showingDocuments = false;
        showingNearServices = false;
        showingExpenseAnalysis = false;
        setStatusBarForNearServices(false);
        applyFragmentHostEdgeToEdge(false);
        binding.appBarLayout.setVisibility(View.VISIBLE);
        binding.scrollContent.setVisibility(View.GONE);
        binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        replaceFragment(RemindersFragment.newInstance());
    }

    private void showStats() {
        showingServiceHistory = false;
        showingDocuments = false;
        showingNearServices = false;
        showingExpenseAnalysis = false;
        setStatusBarForNearServices(false);
        applyFragmentHostEdgeToEdge(false);
        binding.appBarLayout.setVisibility(View.VISIBLE);
        binding.scrollContent.setVisibility(View.GONE);
        binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        replaceFragment(StatisticsFragment.newInstance());
    }

    private void showSettings() {
        showingServiceHistory = false;
        showingDocuments = false;
        showingNearServices = false;
        showingExpenseAnalysis = false;
        setStatusBarForNearServices(false);
        applyFragmentHostEdgeToEdge(false);
        binding.appBarLayout.setVisibility(View.VISIBLE);
        binding.scrollContent.setVisibility(View.GONE);
        binding.remindersFragmentContainer.setVisibility(View.VISIBLE);
        replaceFragment(SettingsFragment.newInstance());
    }

    private void replaceFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.remindersFragmentContainer, fragment)
                .commitNow();
    }

    @Override
    public void onCloseServiceHistory() {
        showHome();
    }

    @Override
    public void onCloseDocuments() {
        showHome();
    }

    @Override
    public void onCloseNearServices() {
        showHome();
    }

    @Override
    public void onCloseExpenseAnalysis() {
        showHome();
    }
    private void yerelAnomaliDinleyicisiBaslat() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.net.ServerSocket serverSocket = new java.net.ServerSocket(8080);
                    while (true) {
                        java.net.Socket socket = serverSocket.accept();

                        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                        String inputLine;

                        while ((inputLine = in.readLine()) != null && !inputLine.isEmpty()) {  }


                        StringBuilder bodyBuilder = new StringBuilder();
                        while (in.ready()) {
                            bodyBuilder.append((char) in.read());
                        }

                        final String gelenAnomaliMetni = bodyBuilder.toString().trim();

                        runOnUiThread(() -> {
                            if (!gelenAnomaliMetni.isEmpty()) {
                                onAnomalyReceived(gelenAnomaliMetni);
                            } else {
                                onAnomalyReceived(getString(R.string.anomaly_default_message));
                            }
                        });

                        // Node-RED el sıkışmasını tamamlamak için HTTP 200 OK yanıtı dönüyoruz
                        java.io.OutputStream output = socket.getOutputStream();
                        output.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nOK".getBytes());
                        output.flush();
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void bildirimiEkranaFirlat(String title, String message) {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "anomali_kanali_yerel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId, "Araç Anomali", android.app.NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_alert_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true);

        notificationManager.notify(2, builder.build());
    }
}
