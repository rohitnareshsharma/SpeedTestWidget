package javanator.test.speedtestwidget;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A placeholder fragment containing a simple view.
 */
public class SpeedTestActivtyFragment extends Fragment implements SpeedTestWidget.ProgressListener{

    private SpeedTestWidget speedTestWidget;

    private TextView tvDownloadSpeed;

    private TextView tvUploadSpeed;

    public SpeedTestActivtyFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_speed_test_activty, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        speedTestWidget = (SpeedTestWidget) view.findViewById(R.id.speed_test_widget);
        tvDownloadSpeed = (TextView) view.findViewById(R.id.tv_download_speed);
        tvUploadSpeed = (TextView) view.findViewById(R.id.tv_upload_speed);

        speedTestWidget.setProgressListener(this);
    }

    @Override
    public void onDownloadProgress(final float downloadSpeedInMbps) {
        tvDownloadSpeed.post(new Runnable() {
            @Override
            public void run() {
                tvDownloadSpeed.setText(downloadSpeedInMbps + "Mbps");
            }
        });
    }

    @Override
    public void onDownloadCompleted() {
        // Doing nothing for now
    }

    @Override
    public void onUploadProgress(final float uploadSpeedInMbps) {
        tvUploadSpeed.post(new Runnable() {
            @Override
            public void run() {
                tvUploadSpeed.setText(uploadSpeedInMbps + "Mbps");
            }
        });
    }

    @Override
    public void onUploadCompleted() {
        // Doing nothing for now
    }
}
