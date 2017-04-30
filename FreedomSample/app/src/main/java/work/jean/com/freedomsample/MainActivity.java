package work.jean.com.freedomsample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import work.jean.com.dashboard.DashboardFragment;
import work.jean.com.home.HomeFragment;
import work.jean.com.notification.NotificationFragment;

public class MainActivity extends AppCompatActivity {


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    updateFragment(new HomeFragment());
                    return true;
                case R.id.navigation_dashboard:
                    updateFragment(new DashboardFragment());
                    return true;
                case R.id.navigation_notifications:
                    updateFragment(new NotificationFragment());
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        updateFragment(new HomeFragment());
    }

    private void updateFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment)
                .commit();
    }

}
