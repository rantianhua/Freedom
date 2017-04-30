package work.jean.com.notification;

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

public class NotificationFragment extends Fragment {

    private TextView tvContent;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        tvContent = (TextView) view.findViewById(R.id.tv_content);
        tvContent.setText("通知");
        return view;
    }
}
