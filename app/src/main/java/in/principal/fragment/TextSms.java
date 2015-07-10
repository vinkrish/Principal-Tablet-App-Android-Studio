package in.principal.fragment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.net.io.CopyStreamAdapter;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.mobileconnectors.s3.transfermanager.Transfer;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;

import in.principal.activity.R;
import in.principal.adapter.Alert;
import in.principal.dao.ClasDao;
import in.principal.dao.SchoolDao;
import in.principal.dao.SectionDao;
import in.principal.dao.StudentsDao;
import in.principal.dao.TempDao;
import in.principal.model.TransferModel;
import in.principal.sqlite.Clas;
import in.principal.sqlite.School;
import in.principal.sqlite.Section;
import in.principal.sqlite.Students;
import in.principal.sqlite.Temp;
import in.principal.sync.StringConstant;
import in.principal.sync.UploadSyncParser;
import in.principal.util.AppGlobal;
import in.principal.util.Constants;
import in.principal.util.PKGenerator;
import in.principal.util.ReplaceFragment;
import in.principal.util.Util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

@SuppressWarnings("deprecation")
@SuppressLint("ClickableViewAccessibility")
public class TextSms extends Fragment implements StringConstant {
    private SQLiteDatabase sqliteDatabase;
    private Button allStudentsBtn, allTeachersBtn, classBtn, sectionBtn, studentBtn, submitBtn, prevBtn;
    private FrameLayout allStudentsFrame, allTeachersFrame;
    private LinearLayout selectionFrame;
    private EditText classSpinner, sectionSpinner, studentSpinner, textSms;
    private int classId, sectionId, principalId, schoolId;
    private ArrayList<Clas> clasList;
    private ArrayList<Section> secList;
    private ArrayList<Integer> classIdList;
    private ArrayList<Integer> secIdList;
    private ArrayList<Integer> studIdList;
    private ArrayList<String> classNameList;
    private ArrayList<String> secNameList;
    private ArrayList<String> studNameList;
    private ArrayList<Long> idList = new ArrayList<>();

    protected boolean[] classSelections;
    protected boolean[] sectionSelections;
    protected boolean[] studentSelections;

    private String ids, zipName, deviceId, messageTo;
    private int target;
    private ProgressDialog progressBar;

    private Context appContext;
    private TransferManager mTransferManager;
    private boolean uploadComplete, exception;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.text_sms, container, false);

        initializeList();

        appContext = AppGlobal.getContext();
        sqliteDatabase = AppGlobal.getSqliteDatabase();

        allStudentsBtn = (Button) view.findViewById(R.id.allStudents);
        allTeachersBtn = (Button) view.findViewById(R.id.allTeachers);
        classBtn = (Button) view.findViewById(R.id.clas);
        sectionBtn = (Button) view.findViewById(R.id.sec);
        studentBtn = (Button) view.findViewById(R.id.stud);
        submitBtn = (Button) view.findViewById(R.id.submit);
        prevBtn = (Button) view.findViewById(R.id.prev);

        allStudentsFrame = (FrameLayout) view.findViewById(R.id.allStudentsFrame);
        allTeachersFrame = (FrameLayout) view.findViewById(R.id.allTeachersFrame);
        selectionFrame = (LinearLayout) view.findViewById(R.id.selectionFrame);

        classSpinner = (EditText) view.findViewById(R.id.classSpinner);
        ;
        sectionSpinner = (EditText) view.findViewById(R.id.secSpinner);
        studentSpinner = (EditText) view.findViewById(R.id.studSpinner);
        textSms = (EditText) view.findViewById(R.id.textSms);

        classSpinner.setOnTouchListener(classTouch);
        sectionSpinner.setOnTouchListener(sectionTouch);
        studentSpinner.setOnTouchListener(studentTouch);

        clasList = ClasDao.selectClas(sqliteDatabase);
        for (Clas c : clasList) {
            classIdList.add(c.getClassId());
            classNameList.add(c.getClassName());
        }
        classSelections = new boolean[clasList.size()];

        ArrayList<School> auth = SchoolDao.selectSchool(sqliteDatabase);
        for (School school : auth) {
            principalId = school.getPrincipalTeacherId();
        }

        allStudentsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deActivate();
                v.setActivated(true);
                submitBtn.setEnabled(true);
                allStudentsFrame.setVisibility(View.VISIBLE);
                target = 0;
            }
        });

        allTeachersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deActivate();
                v.setActivated(true);
                submitBtn.setEnabled(true);
                allTeachersFrame.setVisibility(View.VISIBLE);
                target = 1;
            }
        });

        classBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deActivate();
                v.setActivated(true);
                selectionFrame.setVisibility(View.VISIBLE);
                sectionSpinner.setVisibility(View.INVISIBLE);
                studentSpinner.setVisibility(View.INVISIBLE);
                classSpinner.setText("");
                target = 2;
            }
        });

        sectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deActivate();
                v.setActivated(true);
                selectionFrame.setVisibility(View.VISIBLE);
                sectionSpinner.setVisibility(View.VISIBLE);
                studentSpinner.setVisibility(View.INVISIBLE);
                classSpinner.setText("");
                sectionSpinner.setText("");
                for (int i = 0; i < classSelections.length; i++) {
                    classSelections[i] = false;
                }
                secList.clear();
                secIdList.clear();
                secNameList.clear();
                sectionSelections = new boolean[secIdList.size()];
                target = 3;
            }
        });

        studentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deActivate();
                v.setActivated(true);
                selectionFrame.setVisibility(View.VISIBLE);
                sectionSpinner.setVisibility(View.VISIBLE);
                studentSpinner.setVisibility(View.VISIBLE);
                classSpinner.setText("");
                sectionSpinner.setText("");
                studentSpinner.setText("");
                for (int i = 0; i < classSelections.length; i++) {
                    classSelections[i] = false;
                }
                secList.clear();
                secIdList.clear();
                secNameList.clear();
                sectionSelections = new boolean[secIdList.size()];
                studIdList.clear();
                studNameList.clear();
                studentSelections = new boolean[studIdList.size()];
                target = 4;
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idList.clear();
                if (textSms.getText().toString().equals("")) {
                    Alert a = new Alert(TextSms.this.getActivity());
                    a.showAlert("Please enter message to deliver");
                } else {
                    new CalledFTPSync().execute();
                }
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReplaceFragment.replace(new TextSmsSent(), getFragmentManager());
            }
        });

        classSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ClassDialog classDialog = new ClassDialog();
                classDialog.show(ft, "dialog");
            }
        });

        sectionSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                SectionDialog secFragment = new SectionDialog();
                secFragment.show(ft, "sectionDialog");
            }
        });

        studentSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                StudentDialog studFragment = new StudentDialog();
                studFragment.show(ft, "studentdialog");
            }
        });

        return view;
    }

    private void initializeList() {
        clasList = new ArrayList<>();
        secList = new ArrayList<>();
        classIdList = new ArrayList<>();
        secIdList = new ArrayList<>();
        studIdList = new ArrayList<>();
        classNameList = new ArrayList<>();
        secNameList = new ArrayList<>();
        studNameList = new ArrayList<>();
    }

    private OnTouchListener classTouch = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            int inType = classSpinner.getInputType();
            classSpinner.setInputType(InputType.TYPE_NULL);
            classSpinner.onTouchEvent(event);
            classSpinner.setInputType(inType);
            return false;
        }
    };

    private OnTouchListener sectionTouch = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            int inType = sectionSpinner.getInputType();
            sectionSpinner.setInputType(InputType.TYPE_NULL);
            sectionSpinner.onTouchEvent(event);
            sectionSpinner.setInputType(inType);
            return false;
        }
    };


    private OnTouchListener studentTouch = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            int inType = studentSpinner.getInputType();
            studentSpinner.setInputType(InputType.TYPE_NULL);
            studentSpinner.onTouchEvent(event);
            studentSpinner.setInputType(inType);
            return false;
        }
    };

    private void deActivate() {
        allStudentsBtn.setActivated(false);
        allTeachersBtn.setActivated(false);
        classBtn.setActivated(false);
        sectionBtn.setActivated(false);
        studentBtn.setActivated(false);
        submitBtn.setEnabled(false);
        allStudentsFrame.setVisibility(View.GONE);
        allTeachersFrame.setVisibility(View.GONE);
        selectionFrame.setVisibility(View.GONE);
    }

    public class ClassDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder classBuilder = new AlertDialog.Builder(getActivity());
            classBuilder.setTitle("Classes")
                    .setMultiChoiceItems(classNameList.toArray(new CharSequence[classIdList.size()]), classSelections, new ClassSelectionClickHandler())
                    .setPositiveButton("OK", new ClassButtonClickHandler())
                    .create();
            Dialog d = classBuilder.create();
            return d;
        }
    }

    public class ClassSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener {
        public void onClick(DialogInterface dialog, int clicked, boolean selected) {
            Log.d("i", classNameList.get(clicked) + " selected: " + selected);
        }
    }

    public class ClassButtonClickHandler implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int clicked) {
            switch (clicked) {
                case DialogInterface.BUTTON_POSITIVE:
                    classSpinner.clearFocus();
                    selectedClass();
                    break;
            }
        }
    }

    protected void selectedClass() {
        submitBtn.setEnabled(false);
        boolean isSelected = false;
        StringBuilder sb2 = new StringBuilder();
        if (!sectionSpinner.isShown() && !studentSpinner.isShown()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < classIdList.size(); i++) {
                if (classSelections[i]) {
                    isSelected = true;
                    sb.append(classIdList.get(i) + ",");
                    sb2.append(classNameList.get(i) + ", ");
                }
            }
            if (isSelected) {
                classSpinner.setText(sb2.substring(0, sb2.length() - 2));
                ids = sb.substring(0, sb.length() - 1);
                submitBtn.setEnabled(true);
            } else {
                classSpinner.setText("");
            }
        } else if (sectionSpinner.isShown()) {
            for (int i = 0; i < classIdList.size(); i++) {
                if (classSelections[i]) {
                    isSelected = true;
                    classId = classIdList.get(i);
                    sb2.append(classNameList.get(i));
                    break;
                }
            }
            if (!isSelected) {
                classId = 0;
                classSpinner.setText("");
            } else {
                classSpinner.setText(sb2.toString());
            }
            sectionSpinner.setText("");
            secList.clear();
            secIdList.clear();
            secNameList.clear();
            secList = SectionDao.selectSection(classId, sqliteDatabase);
            for (Section s : secList) {
                secIdList.add(s.getSectionId());
                secNameList.add(s.getSectionName());
            }
            sectionSelections = new boolean[secIdList.size()];

            studIdList.clear();
            studNameList.clear();
            studentSelections = new boolean[studIdList.size()];
        }
    }

    public class SectionDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Sections")
                    .setMultiChoiceItems(secNameList.toArray(new CharSequence[secIdList.size()]), sectionSelections, new SectionSelectionClickHandler())
                    .setPositiveButton("OK", new SectionButtonClickHandler())
                    .create();
        }
    }

    public class SectionSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener {
        public void onClick(DialogInterface dialog, int clicked, boolean selected) {
            Log.d("i", secNameList.get(clicked) + " selected: " + selected);
        }
    }

    public class SectionButtonClickHandler implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int clicked) {
            switch (clicked) {
                case DialogInterface.BUTTON_POSITIVE:
                    sectionSpinner.clearFocus();
                    selectedSection();
                    break;
            }
        }
    }

    protected void selectedSection() {
        submitBtn.setEnabled(false);
        boolean isSelected = false;
        StringBuilder sb2 = new StringBuilder();
        if (!studentSpinner.isShown()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < secIdList.size(); i++) {
                if (sectionSelections[i]) {
                    isSelected = true;
                    sb.append(secIdList.get(i) + ",");
                    sb2.append(secNameList.get(i) + ", ");
                }
            }
            if (isSelected) {
                sectionSpinner.setText(sb2.substring(0, sb2.length() - 2));
                ids = sb.substring(0, sb.length() - 1);
                submitBtn.setEnabled(true);
            } else {
                sectionSpinner.setText("");
            }
        } else {
            for (int i = 0; i < secIdList.size(); i++) {
                if (sectionSelections[i]) {
                    isSelected = true;
                    sectionId = secIdList.get(i);
                    sb2.append(secNameList.get(i));
                    break;
                }
            }
            if (!isSelected) {
                sectionId = 0;
                sectionSpinner.setText("");
            } else {
                sectionSpinner.setText(sb2.toString());
            }
            studentSpinner.setText("");
            studIdList.clear();
            studNameList.clear();
            List<Students> studentList = StudentsDao.selectStudents(sectionId, sqliteDatabase);
            for (Students s : studentList) {
                studIdList.add(s.getStudentId());
                studNameList.add(s.getName());
            }
            studentSelections = new boolean[studIdList.size()];
        }
    }

    public class StudentDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Students")
                    .setMultiChoiceItems(studNameList.toArray(new CharSequence[studIdList.size()]), studentSelections, new StudentSelectionClickHandler())
                    .setPositiveButton("OK", new StudentButtonClickHandler())
                    .create();
        }
    }

    public class StudentSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener {
        public void onClick(DialogInterface dialog, int clicked, boolean selected) {
            Log.d("i", studNameList.get(clicked) + " selected: " + selected);
        }
    }

    public class StudentButtonClickHandler implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int clicked) {
            switch (clicked) {
                case DialogInterface.BUTTON_POSITIVE:
                    studentSpinner.clearFocus();
                    selectedStudent();
                    break;
            }
        }
    }

    protected void selectedStudent() {
        submitBtn.setEnabled(false);
        boolean isSelected = false;
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < studIdList.size(); i++) {
            if (studentSelections[i]) {
                isSelected = true;
                sb.append(studIdList.get(i) + ",");
                sb2.append(studNameList.get(i) + ", ");
            }
        }
        if (isSelected) {
            studentSpinner.setText(sb2.substring(0, sb2.length() - 2));
            ids = sb.substring(0, sb.length() - 1);
            submitBtn.setEnabled(true);
        } else {
            studentSpinner.setText("");
        }
    }

    class CalledFTPSync extends AsyncTask<String, Integer, String> {
        CopyStreamAdapter streamListener;
        private JSONObject jsonReceived;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar = new ProgressDialog(TextSms.this.getActivity());
            progressBar.setCancelable(false);
            progressBar.setMessage("Sending SMS...");
            //	progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            //	progressBar.setProgress(0);
            //	progressBar.setMax(100);
            progressBar.show();
        }

		/*@Override
        protected void onProgressUpdate(Integer... progress) {
			progressBar.setProgress(progress[0]);
		}*/

        protected String doInBackground(String... arg0) {
            mTransferManager = new TransferManager(Util.getCredProvider(appContext));
            uploadComplete = false;
            exception = false;
            prepareIds();
            createUploadFile();

            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/Upload");

            File file = new File(dir, zipName);
            UploadModel model = new UploadModel(appContext, zipName, mTransferManager);
            model.upload();

            while (!uploadComplete) {
                Log.d("upload", "...");
            }

            if (!exception) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("school", schoolId);
                    jsonObject.put("tab_id", deviceId);
                    jsonObject.put("file_name", zipName.substring(0, zipName.length() - 3) + "sql");
                    jsonReceived = UploadSyncParser.makePostRequest(acknowledge_uploaded_file, jsonObject);
                    if (jsonReceived.getInt(TAG_SUCCESS) == 1) {
                        file.delete();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
               /* try {
                    sqliteDatabase.execSQL("insert into textsms(Message, Date, MessageTo, Ids) values('" + textSms.getText().toString().replaceAll("\n", "-") + "','" +
                            getToday() + "','" + messageTo + "','" + ids + "')");
                } catch (SQLException e) {
                }*/
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressBar.dismiss();
            ReplaceFragment.clearBackStack(getFragmentManager());
            ReplaceFragment.replace(new Dashbord(), getFragmentManager());
        }
    }

    private void prepareIds() {
        if (target == 0) {
            messageTo = "All Students";
            Cursor c = sqliteDatabase.rawQuery("select Mobile1 from students", null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                idList.add(c.getLong(c.getColumnIndex("Mobile1")));
                c.moveToNext();
            }
            c.close();
        } else if (target == 1) {
            messageTo = "All Teachers";
            Cursor c = sqliteDatabase.rawQuery("select Mobile from teacher", null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                idList.add(c.getLong(c.getColumnIndex("Mobile")));
                c.moveToNext();
            }
            c.close();
        } else if (target == 2) {
            messageTo = "Class";
            Cursor c = sqliteDatabase.rawQuery("select Mobile1 from students where ClassId in (" + ids + ")", null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                idList.add(c.getLong(c.getColumnIndex("Mobile1")));
                c.moveToNext();
            }
            c.close();
        } else if (target == 3) {
            messageTo = "Section";
            Cursor c = sqliteDatabase.rawQuery("select Mobile1 from students where SectionId in (" + ids + ")", null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                idList.add(c.getLong(c.getColumnIndex("Mobile1")));
                c.moveToNext();
            }
            c.close();
        } else if (target == 4) {
            messageTo = "Student";

            Cursor c = sqliteDatabase.rawQuery("select Mobile1 from students where StudentId in (" + ids + ")", null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                idList.add(c.getLong(c.getColumnIndex("Mobile1")));
                c.moveToNext();
            }
            c.close();
        }
    }

    private void createUploadFile() {
        Temp t = TempDao.selectTemp(sqliteDatabase);
        deviceId = t.getDeviceId();
        schoolId = t.getSchoolId();
        long timeStamp = PKGenerator.getPrimaryKey();
        zipName = timeStamp + "_" + deviceId + "_" + schoolId + ".zip";
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + "/Upload");
        dir.mkdirs();
        File file = new File(dir, timeStamp + "_" + deviceId + "_" + schoolId + ".sql");
        file.delete();
        try {
            file.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            for (Long id : idList) {
                writer.write("insert into queue_transaction(SchoolId, Phone, Message, UserId, Role) values(" + schoolId + ",'" + id + "','" + textSms.getText().toString().replaceAll("\n", "-") + "'," + principalId + ", 'Principal');");
                writer.newLine();
            }
            writer.close();
            FileOutputStream fileOutputStream = new FileOutputStream(new File(dir, zipName));
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zipOutputStream.putNextEntry(zipEntry);
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buf)) > 0) {
                zipOutputStream.write(buf, 0, bytesRead);
            }
            fileInputStream.close();
            zipOutputStream.closeEntry();
            zipOutputStream.close();
            fileOutputStream.close();
            file.delete();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    private String getToday() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date today = new Date();
        return dateFormat.format(today);
    }

    public class UploadModel extends TransferModel {
        private String fileNam;
        private Upload mUpload;
        private ProgressListener mListener;
        private Status mStatus;

        public UploadModel(Context context, String key, TransferManager manager) {
            super(context, Uri.parse(key), manager);
            fileNam = key;
            mStatus = Status.IN_PROGRESS;
            mListener = new ProgressListener() {
                @Override
                public void progressChanged(ProgressEvent event) {
                    if (event.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
                        mStatus = Status.COMPLETED;
                        Log.d("upload", "complete");
                        uploadComplete = true;
                    } else if (event.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
                        exception = true;
                        uploadComplete = true;
                    } else if (event.getEventCode() == ProgressEvent.CANCELED_EVENT_CODE) {
                        exception = true;
                        uploadComplete = true;
                    }
                }
            };
        }

        @Override
        public Status getStatus() {
            return mStatus;
        }

        @Override
        public Transfer getTransfer() {
            return mUpload;
        }

        public void upload() {
            try {
                File root = android.os.Environment.getExternalStorageDirectory();
                File dir = new File(root.getAbsolutePath() + "/Upload");
                File file = new File(dir, fileNam);
                mUpload = getTransferManager().upload(
                        Constants.BUCKET_NAME.toLowerCase(Locale.US), "upload/zipped_folder/" + fileNam,
                        file);
                mUpload.addProgressListener(mListener);
            } catch (Exception e) {
            }
        }

        @Override
        public void abort() {
        }

        @Override
        public void pause() {
        }

        @Override
        public void resume() {
        }
    }

}
