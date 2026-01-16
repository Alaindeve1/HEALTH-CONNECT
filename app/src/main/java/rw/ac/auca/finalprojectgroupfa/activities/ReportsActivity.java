package rw.ac.auca.finalprojectgroupfa.activities;

import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.adapters.ReportAdapter;
import rw.ac.auca.finalprojectgroupfa.models.ConsultationRequest;
import rw.ac.auca.finalprojectgroupfa.viewmodels.ReportsViewModel;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity implements ReportAdapter.OnReportActionListener {
    private ReportsViewModel viewModel;
    private ReportAdapter adapter;
    private RecyclerView reportsRecyclerView;
    private TextView emptyReportsText;
    private ProgressBar loadingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Medical Reports");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        setupViewModel();
    }

    private void initializeViews() {
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        emptyReportsText = findViewById(R.id.emptyReportsText);
        loadingProgress = findViewById(R.id.loadingProgress);

        adapter = new ReportAdapter(this);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportsRecyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ReportsViewModel.class);
        loadingProgress.setVisibility(View.VISIBLE);

        viewModel.getCompletedConsultations().observe(this, reports -> {
            loadingProgress.setVisibility(View.GONE);
            if (reports != null && !reports.isEmpty()) {
                adapter.setReports(reports);
                emptyReportsText.setVisibility(View.GONE);
                reportsRecyclerView.setVisibility(View.VISIBLE);
            } else {
                emptyReportsText.setVisibility(View.VISIBLE);
                reportsRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDownloadReport(ConsultationRequest report) {
        generatePdf(report);
    }

    private void generatePdf(ConsultationRequest report) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        titlePaint.setTextSize(24);
        titlePaint.setColor(Color.BLACK);

        paint.setTextSize(14);
        paint.setColor(Color.BLACK);

        int x = 50;
        int y = 50;

        canvas.drawText("Medical Consultation Report", x, y, titlePaint);
        y += 50;

        canvas.drawText("Patient Name: " + (report.getPatientName() != null ? report.getPatientName() : "N/A"), x, y, paint);
        y += 30;
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateStr = report.getCompletedAt() != null ? sdf.format(report.getCompletedAt()) : "N/A";
        canvas.drawText("Date: " + dateStr, x, y, paint);
        y += 30;

        canvas.drawText("Doctor: " + (report.getAssignedDoctorName() != null ? report.getAssignedDoctorName() : "N/A"), x, y, paint);
        y += 30;

        canvas.drawText("Reason/Symptoms: " + (report.getReason() != null ? report.getReason() : "N/A"), x, y, paint);
        y += 50;
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawLine(50, y, 545, y, paint);
        y += 30;
        
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText("This is an official medical record generated by Telehealth App.", x, y, paint);

        pdfDocument.finishPage(page);

        String fileName = "Report_" + report.getPatientName() + "_" + System.currentTimeMillis() + ".pdf";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (uri != null) {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    pdfDocument.writeTo(outputStream);
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    Toast.makeText(this, "Report saved to Downloads", Toast.LENGTH_LONG).show();
                }
            } else {
                // For older versions, would need WRITE_EXTERNAL_STORAGE permission
                // For this project, assuming modern Android or emulator
                Toast.makeText(this, "Saving not supported on this Android version without permissions", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }
}
