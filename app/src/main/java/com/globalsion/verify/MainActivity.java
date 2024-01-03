package com.globalsion.verify;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.globalsion.verify.helpers.ExportFunc;
import com.google.android.material.snackbar.Snackbar;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import com.globalsion.verify.helpers.DBHelper;
import com.globalsion.verify.helpers.ExcelHelper;

public class MainActivity extends AppCompatActivity {

    static {
        System.setProperty(
                "org.apache.poi.javax.xml.stream.XMLInputFactory",
                "com.fasterxml.aalto.stax.InputFactoryImpl"
        );
        System.setProperty(
                "org.apache.poi.javax.xml.stream.XMLOutputFactory",
                "com.fasterxml.aalto.stax.OutputFactoryImpl"
        );
        System.setProperty(
                "org.apache.poi.javax.xml.stream.XMLEventFactory",
                "com.fasterxml.aalto.stax.EventFactoryImpl"
        );
    }

    DBHelper controller = new DBHelper(this);
    CustomAdapter adapter;

    EditText lbl;
    Button btnCheck;

    SwipeRefreshLayout swipeRefreshLayout;
    ListView lv;

    private static final int PERMISSION_REQUEST_MEMORY_ACCESS = 0;
    private static String fileType = "";
    private View mLayout;
    private static final String extensionXLS = "XLS";
    private static final String extensionXLXS = "XLXS";
    ActivityResultLauncher<Intent> filePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lbl = findViewById(R.id.txtresulttext);
        btnCheck = findViewById(R.id.btnCheck);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        lv = findViewById(R.id.lstView);
        mLayout = findViewById(R.id.main_layout);

        lbl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used in this case
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used in this case
            }

            @Override
            public void afterTextChanged(Editable editable) {
                boolean isEmpty = lbl.getText().toString().isEmpty();
                if (isEmpty) {
                    // Text is empty, do nothing
                } else {
                    updateCheckboxState(lbl);
                }
            }
        });

        ArrayList<HashMap<String, String>> myList = controller.getProducts();
        adapter = new CustomAdapter(MainActivity.this, myList,
                R.layout.lst_template, new String[]{DBHelper.Company, DBHelper.Product, DBHelper.Code, DBHelper.Status},
                new int[]{R.id.txtproductcompany, R.id.txtproductname, R.id.txtproductprice, R.id.checkbox});

        lv.setAdapter(adapter);

        btnCheck.setOnClickListener(new BtnCheckClickListener(lbl));

        filePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        Intent intent1 = result.getData();

                        Uri uri = intent1.getData();
                        ReadExcelFile(MainActivity.this
                                , uri);
                    }
                });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                FillList();

                swipeRefreshLayout.setRefreshing(false);
            }
        });

        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Not needed for this case
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // Check if the first visible item index is 0 and the top of the ListView is reached
                boolean atTop = firstVisibleItem == 0 && lv.getChildAt(0) != null && lv.getChildAt(0).getTop() == 0;
                swipeRefreshLayout.setEnabled(atTop);
            }
        });
        FillList();
    }

    private void updateCheckboxState(EditText lbl) {
        // Check if the text is being updated programmatically
        String codeToCheck = lbl.getText().toString();

        if (controller.setCheck(codeToCheck)) {
            int status = controller.getStatus(codeToCheck);

            int newStatus = (status == 1) ? 0 : 1;
            controller.updateStatus(codeToCheck, newStatus);

            adapter.notifyDataSetChanged();

            if (newStatus == 1) {
                Toast.makeText(MainActivity.this, "Code " + codeToCheck + " checked", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Code " + codeToCheck + " unchecked", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "Code " + codeToCheck + " does not exist.", Toast.LENGTH_SHORT).show();
        }

        FillList();
        lbl.setText("");
        return;
    }

    private class BtnCheckClickListener implements View.OnClickListener {

        private EditText txtresulttext;

        public BtnCheckClickListener(EditText txtresulttext) {
            this.txtresulttext = txtresulttext;
        }

        @Override
        public void onClick(View v) {
            String codeToCheck = txtresulttext.getText().toString();

            if (controller.setCheck(codeToCheck)) {
                int status = controller.getStatus(codeToCheck);

                int newStatus = (status == 1) ? 0 : 1;
                controller.updateStatus(codeToCheck, newStatus);

                adapter.notifyDataSetChanged();

                if (newStatus == 1) {
                    Toast.makeText(MainActivity.this, "Code " + codeToCheck + " checked", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Code " + codeToCheck + " unchecked", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Code " + codeToCheck + " does not exist.", Toast.LENGTH_SHORT).show();
            }
            FillList();
            txtresulttext.setText("");
        }
    }

    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();

        int position = lv.getPositionForView(view);

        HashMap<String, String> item = (HashMap<String, String>) lv.getAdapter().getItem(position);

        String codeToCheck = item.get(DBHelper.Code);

        int newStatus = checked ? 1 : 0;
        controller.updateStatus(codeToCheck, newStatus);

        if (newStatus == 1) {
            Toast.makeText(this, "Code " + codeToCheck + " checked", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Code " + codeToCheck + " unchecked", Toast.LENGTH_SHORT).show();
        }
        FillList();
    }

    private boolean CheckPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            Snackbar.make(mLayout, R.string.storage_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requestStoragePermission();
                }
            }).show();

            return false;
        }
    }

    public void FillList() {
        try {
            ArrayList<HashMap<String, String>> myList = controller.getProducts();
            if (myList.size() != 0) {
                CustomAdapter adapter = new CustomAdapter(MainActivity.this, myList,
                        R.layout.lst_template, new String[]{DBHelper.Company, DBHelper.Product, DBHelper.Code, DBHelper.Status},
                        new int[]{R.id.txtproductcompany, R.id.txtproductname, R.id.txtproductprice, R.id.checkbox});

                lv.setAdapter(adapter);
            }
        } catch (Exception ex) {
            Toast("FillList error: " + ex.getMessage(), ex);
        }
    }

    public void ReadExcelFile(Context context, Uri uri) {
        try {
            InputStream inStream;
            Workbook wb = null;

            try {
                inStream = context.getContentResolver().openInputStream(uri);

                if (fileType == extensionXLS)
                    wb = new HSSFWorkbook(inStream);
                else
                    wb = new XSSFWorkbook(inStream);

                inStream.close();
            } catch (IOException e) {
                lbl.setText("First " + e.getMessage().toString());
                e.printStackTrace();
            }

            DBHelper dbAdapter = new DBHelper(this);
            Sheet sheet1 = wb.getSheetAt(0);

            dbAdapter.open();
            dbAdapter.delete();
            dbAdapter.close();
            dbAdapter.open();
            ExcelHelper.insertExcelToSqlite(dbAdapter, sheet1);

            dbAdapter.close();

            FillList();
        } catch (Exception ex) {
            Toast("ReadExcelFile Error:" + ex.getMessage(), ex);
        }
    }

    public void ChooseFile() {
        try {
            Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileIntent.addCategory(Intent.CATEGORY_OPENABLE);

            if (fileType == extensionXLS)
                fileIntent.setType("application/vnd.ms-excel");
            else
                fileIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            filePicker.launch(fileIntent);
        } catch (Exception ex) {
            Toast("ChooseFile error: " + ex.getMessage(), ex);

        }
    }

    void Toast(String message, Exception ex) {
        if (ex != null)
            Log.e("Error", ex.getMessage());
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_MEMORY_ACCESS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                OpenFilePicker();
            } else {
                Snackbar.make(mLayout, R.string.storage_access_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void requestStoragePermission() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_MEMORY_ACCESS);

        } else {
            Snackbar.make(mLayout, R.string.storage_unavailable, Snackbar.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_MEMORY_ACCESS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_import_xls:
                fileType = extensionXLS;
                OpenFilePicker();

                return true;

            case R.id.action_import_xlxs:
                fileType = extensionXLXS;
                OpenFilePicker();

                return true;

            case R.id.action_export:

                ExportFunc exCSV = new ExportFunc();
                try {
                    exCSV.exportCSV(this);
                    FillList();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void OpenFilePicker() {
        try {
            if (CheckPermission()) {
                ChooseFile();
            }
        } catch (ActivityNotFoundException e) {
            lbl.setText("Error: " + e.getMessage());
        }
    }
}