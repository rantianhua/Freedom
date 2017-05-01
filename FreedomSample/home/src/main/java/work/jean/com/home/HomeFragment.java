package work.jean.com.home;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by rantianhua on 17/4/30.
 */

public class HomeFragment extends Fragment {

    private TextView tvContent;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        tvContent = (TextView) root.findViewById(R.id.tv_content);
        tvContent.setText(R.string.home);
        return root;
    }
}
