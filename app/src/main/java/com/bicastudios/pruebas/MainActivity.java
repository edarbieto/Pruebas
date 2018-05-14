package com.bicastudios.pruebas;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.listview)
    ListView listView;
    @BindView(R.id.linearlayout)
    LinearLayout linearLayout;
    List<String> songs;
    int current_id;
    MediaPlayer mediaPlayer;
    Visualizer visualizer;
    VisualizerView visualizerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        visualizerView = new VisualizerView(this);
        linearLayout.addView(visualizerView);
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        listSongs();
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.simple_list_item_1,
                                android.R.id.text1, songs);
                        listView.setAdapter(adapter);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                current_id = (int) id;
                Uri uri = Uri.parse(songs.get(current_id));
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    visualizer.setEnabled(false);
                    visualizer.release();
                    visualizer = null;
                }
                mediaPlayer = MediaPlayer.create(MainActivity.this, uri);
                mediaPlayer.start();
                visualizer = new Visualizer(mediaPlayer.getAudioSessionId());
                visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[0]);
                visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                    @Override
                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                        visualizerView.updateVisualizer(waveform);
                    }

                    @Override
                    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {

                    }
                }, Visualizer.getMaxCaptureRate(), true, false);
                visualizer.setEnabled(true);
            }
        });
    }

    private void listSongs() {
        songs = new ArrayList<>();
        Cursor cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media.DATA},
                null,
                null,
                null);
        while (cursor.moveToNext()) {
            songs.add(cursor.getString(0));
        }
        cursor.close();
    }
}

class VisualizerView extends View {
    private byte[] bytes;
    private float[] points;
    private Rect rect = new Rect();
    private Paint paint = new Paint();

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    private void init() {
        bytes = null;
        paint.setStrokeWidth(1);
        paint.setColor(Color.BLACK);
    }

    public void updateVisualizer(byte[] bytes) {
        this.bytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bytes == null) {
            return;
        }
        if (points == null || points.length < bytes.length * 4) {
            points = new float[bytes.length * 4];
        }
        rect.set(0, 0, getWidth(), getHeight());
        for (int i = 0; i < bytes.length - 1; i++) {
            points[i * 4] = rect.width() * i / (bytes.length - 1);
            points[i * 4 + 1] = rect.height() / 2
                    + ((byte) (bytes[i] + 128)) * (rect.height() / 2) / 128;
            points[i * 4 + 2] = rect.width() * (i + 1) / (bytes.length - 1);
            points[i * 4 + 3] = rect.height() / 2
                    + ((byte) (bytes[i + 1] + 128)) * (rect.height() / 2)
                    / 128;
        }
        canvas.drawLines(points, paint);
    }
}