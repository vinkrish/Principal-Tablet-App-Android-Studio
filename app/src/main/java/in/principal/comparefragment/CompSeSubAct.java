package in.principal.comparefragment;

import java.util.ArrayList;
import java.util.List;

import in.principal.activity.R;
import in.principal.adapter.CompList1;
import in.principal.adapter.CompList2;
import in.principal.dao.ActivitiDao;
import in.principal.dao.ExamsDao;
import in.principal.dao.ExmAvgDao;
import in.principal.dao.SectionDao;
import in.principal.dao.SubjectsDao;
import in.principal.dao.TempDao;
import in.principal.examfragment.SeSubAct;
import in.principal.sqlite.Circle;
import in.principal.sqlite.Activiti;
import in.principal.sqlite.AdapterOverloaded;
import in.principal.sqlite.Section;
import in.principal.sqlite.SqlDbHelper;
import in.principal.sqlite.Temp;
import in.principal.util.AppGlobal;
import in.principal.util.CommonDialogUtils;
import in.principal.util.ReplaceFragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by vinkrish.
 * Needs refactoring - Don't grab a beer before coding, serious thought needed.
 */

public class CompSeSubAct extends Fragment {
    private Context context;
    private Activity act;
    private SqlDbHelper sqlHandler;
    private String className, examName;
    private int subjectId, classId, isCompare = 0;
    private long examId;
    private List<Section> secList = new ArrayList<>();
    private static List<Integer> secIdList = new ArrayList<>();
    private List<String> secNameList = new ArrayList<>();
    private List<String> secNameList2 = new ArrayList<>();
    private List<Integer> actIdList = new ArrayList<>();
    private List<String> actNameList = new ArrayList<>();
    private List<Integer> avgList = new ArrayList<>();
    private List<Activiti> activitiList = new ArrayList<>();
    private static List<Integer> secIdList2 = new ArrayList<>();
    private List<Integer> progressList1 = new ArrayList<>();
    private List<Integer> progressList2 = new ArrayList<>();
    private List<Integer> progressList3 = new ArrayList<>();
    private List<Long> actIdList1 = new ArrayList<>();
    private List<Long> actIdList2 = new ArrayList<>();
    private List<Long> actIdList3 = new ArrayList<>();
    private TextView comp1Sec1, comp1Sec2, comp2Sec1, comp2Sec2, comp2Sec3;
    private int noOfSections = 0;
    private SQLiteDatabase sqliteDatabase;
    private ListView lv;
    private ArrayList<AdapterOverloaded> amrList = new ArrayList<AdapterOverloaded>();
    private CompList1 compAdapter1;
    private CompList2 compAdapter2;
    private ArrayList<Circle> circleArrayGrid = new ArrayList<Circle>();
    private CircleAdapter cA;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.comp_act_secsub, container, false);
        FrameLayout fl2 = (FrameLayout) view.findViewById(R.id.fl_comp1);
        FrameLayout fl3 = (FrameLayout) view.findViewById(R.id.fl_comp2);
        comp1Sec1 = (TextView) view.findViewById(R.id.comp1sec1);
        comp1Sec2 = (TextView) view.findViewById(R.id.comp1sec2);
        comp2Sec1 = (TextView) view.findViewById(R.id.comp2sec1);
        comp2Sec2 = (TextView) view.findViewById(R.id.comp2sec2);
        comp2Sec3 = (TextView) view.findViewById(R.id.comp2sec3);
        lv = (ListView) view.findViewById(R.id.list);

        act = AppGlobal.getActivity();
        context = AppGlobal.getContext();
        sqlHandler = AppGlobal.getSqlDbHelper();
        sqliteDatabase = AppGlobal.getSqliteDatabase();
        ;

        Temp t = TempDao.selectTemp(sqliteDatabase);
        subjectId = t.getSubjectId();
        classId = t.getClassId();
        className = t.getClassName();
        examId = t.getExamId();

        GridView gridView = (GridView) view.findViewById(R.id.gridView);
        cA = new CircleAdapter(context, R.layout.section_grid, circleArrayGrid);
        gridView.setAdapter(cA);

        examName = ExamsDao.selectExamName(examId, sqliteDatabase);

        clearList();

        Cursor c = sqliteDatabase.rawQuery("select * from comp", null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            secIdList2.add(c.getInt(c.getColumnIndex("SecId")));
            isCompare = c.getInt(c.getColumnIndex("IsCompare"));
            noOfSections += 1;
            c.moveToNext();
        }
        c.close();

        for (Integer id : secIdList2) {
            secNameList2.add(SectionDao.getSecName(id, sqliteDatabase));
        }

        Button perfClas = (Button) view.findViewById(R.id.seClass);
        perfClas.setText("Class " + className);
        Button SeBut = (Button) view.findViewById(R.id.se);
        SeBut.setText(examName);

        Button reeturn = (Button) view.findViewById(R.id.reeturn);
        reeturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReplaceFragment.replace(new SeSubAct(), getFragmentManager());
            }
        });

        Button go = (Button) view.findViewById(R.id.go);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noOfSections < 2 || noOfSections > 3) {
                    CommonDialogUtils.displayAlertWhiteDialog(act, "select min of 2 and max of 3 sections");
                } else {
                    sqliteDatabase.execSQL("update comp set IsCompare=1");
                    ReplaceFragment.replace(new CompSeSubAct(), getFragmentManager());
                }
            }
        });

        TextView subj = (TextView) view.findViewById(R.id.subinfo);
        subj.setText(SubjectsDao.getSubjectName(subjectId, sqliteDatabase));

        secList = SectionDao.selectSection(classId, sqliteDatabase);
        for (Section s : secList) {
            secIdList.add(s.getSectionId());
            secNameList.add(s.getSectionName());
        }

        if (isCompare == 1) {
            sqliteDatabase.execSQL("delete from comp");
            int loop = 0;
            for (Integer secId : secIdList2) {
                activitiList.clear();
                activitiList = ActivitiDao.selectActiviti(examId, subjectId, secId, sqliteDatabase);
                if (loop == 0) {
                    for (Activiti at : activitiList) {
                        actNameList.add(at.getActivityName());
                        actIdList1.add(at.getActivityId());
                    }
                } else if (loop == 1) {
                    for (Activiti at : activitiList) {
                        actIdList2.add(at.getActivityId());
                    }
                } else if (loop == 2) {
                    for (Activiti at : activitiList) {
                        actIdList3.add(at.getActivityId());
                    }
                }
                loop++;
            }
            if (noOfSections == 2) {
                fl2.setVisibility(View.VISIBLE);
                updateCompare2();
            } else {
                fl3.setVisibility(View.VISIBLE);
                updateCompare3();
            }
        }

        updateView();

        return view;
    }

    private static boolean isSecIdPresent(int secId) {
        if (secIdList2.contains(secId)) {
            return true;
        } else {
            return false;
        }
    }

    public void clearList() {
        activitiList.clear();
        actIdList.clear();
        actNameList.clear();
        avgList.clear();
        secIdList2.clear();
        progressList1.clear();
        progressList2.clear();
        progressList3.clear();
        actIdList1.clear();
        actIdList2.clear();
        actIdList3.clear();
        secIdList.clear();
    }

    private void updateView() {
        for (int loop = 0; loop < 4 && loop < secList.size(); loop++) {
            final Section s = secList.get(loop);
            int per = ExmAvgDao.seSecSubAvg(s.getSectionId(), subjectId, sqliteDatabase);
            if (secIdList2.contains(s.getSectionId())) {
                Circle c = new Circle(per, s.getSectionName(), true);
                circleArrayGrid.add(c);
            } else {
                Circle c = new Circle(per, s.getSectionName(), false);
                circleArrayGrid.add(c);
            }
            cA.notifyDataSetChanged();
            /*tvv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean added = isSecIdPresent(c.getSectionId());
					if(added){
						sqlHandler.removeComp(c.getSectionId());
						Fragment fragment = new CompSeSubAct();
						getFragmentManager()
						.beginTransaction()
						.setCustomAnimations(animator.fade_in,animator.fade_out)
						.replace(R.id.content_frame, fragment).addToBackStack(null).commit();

					}else{
						sqlHandler.insertComp(c.getSectionId());
						Fragment fragment = new CompSeSubAct();
						getFragmentManager()
						.beginTransaction()
						.setCustomAnimations(animator.fade_in,animator.fade_out)
						.replace(R.id.content_frame, fragment).addToBackStack(null).commit();
					}
				}
			});*/
        }
    }

    private void updateCompare2() {
        comp1Sec1.setText("Section " + secNameList2.get(0));
        comp1Sec2.setText("Section " + secNameList2.get(1));
        for (int i = 0; i < secIdList2.size(); i++) {
            if (i == 0) {
                for (Long actId : actIdList1) {
                    progressList1.add(ActivitiDao.getActAvg(actId, sqliteDatabase));
                }
            } else {
                for (Long actId : actIdList2) {
                    progressList2.add(ActivitiDao.getActAvg(actId, sqliteDatabase));
                }
            }
        }
        for (int i = 0; i < actIdList1.size(); i++) {
            amrList.add(new AdapterOverloaded(actNameList.get(i), progressList1.get(i), progressList2.get(i)));
        }
        compAdapter1 = new CompList1(context, R.layout.comp_list1, amrList);
        lv.setAdapter(compAdapter1);
    }

    private void updateCompare3() {
        comp2Sec1.setText("Section " + secNameList2.get(0));
        comp2Sec2.setText("Section " + secNameList2.get(1));
        comp2Sec3.setText("Section " + secNameList2.get(2));
        for (int i = 0; i < secIdList2.size(); i++) {
            if (i == 0) {
                for (Long actId : actIdList1) {
                    progressList1.add(ActivitiDao.getActAvg(actId, sqliteDatabase));
                }
            } else if (i == 1) {
                for (Long actId : actIdList2) {
                    progressList2.add(ActivitiDao.getActAvg(actId, sqliteDatabase));
                }
            } else {
                for (Long actId : actIdList3) {
                    progressList3.add(ActivitiDao.getActAvg(actId, sqliteDatabase));
                }
            }
        }
        for (int j = 0; j < actIdList1.size(); j++) {
            amrList.add(new AdapterOverloaded(actNameList.get(j), progressList1.get(j), progressList2.get(j), progressList3.get(j)));
        }
        compAdapter2 = new CompList2(context, R.layout.comp_list2, amrList);
        lv.setAdapter(compAdapter2);
    }

    public class CircleAdapter extends ArrayAdapter<Circle> {
        Context context;
        int layoutResourceId;
        ArrayList<Circle> data = new ArrayList<>();
        protected ListView mListView;
        private LayoutInflater inflater = null;

        public CircleAdapter(Context context, int layoutResourceId, ArrayList<Circle> gridArray) {
            super(context, layoutResourceId, gridArray);
            this.context = context;
            this.layoutResourceId = layoutResourceId;
            this.data = gridArray;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            RecordHolder holder;

            if (row == null) {
                row = inflater.inflate(layoutResourceId, parent, false);
                holder = new RecordHolder();
                holder.secTxtBlack = (TextView) row.findViewById(R.id.sectionBlack);
                holder.ll = (LinearLayout) row.findViewById(R.id.sec_grid);
                row.setTag(holder);
            } else holder = (RecordHolder) row.getTag();

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            FrameLayout fl = (FrameLayout) row.findViewById(R.id.fl);

            Circle gridItem = data.get(position);
            SampleView sV = new SampleView(context, gridItem.getProgressInt(), gridItem.isSelected());
            holder.secTxtBlack.setText(gridItem.getSec());
            if (gridItem.isSelected()) {
                holder.ll.setActivated(true);
            }
            fl.addView(sV, layoutParams);

            holder.ll.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int sectionId = secIdList.get(position);
                    boolean added = isSecIdPresent(sectionId);
                    if (added) {
                        sqlHandler.removeComp(sectionId);
                        ReplaceFragment.replace(new CompSeSubAct(), getFragmentManager());
                    } else {
                        sqlHandler.insertComp(sectionId);
                        ReplaceFragment.replace(new CompSeSubAct(), getFragmentManager());
                    }
                }
            });
            return row;
        }

        public class RecordHolder {
            TextView secTxtBlack;
            LinearLayout ll;
        }

        private class SampleView extends View {
            Paint p, defaultPaint, circlePaint;
            RectF rectF;
            int localInt;
            boolean selected;

            public SampleView(Context context, int i, boolean b) {
                super(context);
                setFocusable(true);
                localInt = i;
                selected = b;
                init();
            }

            public void init() {
                circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                circlePaint.setStyle(Paint.Style.FILL);
                circlePaint.setColor(Color.BLACK);
                p = new Paint();
                defaultPaint = new Paint();
                defaultPaint.setAntiAlias(true);
                defaultPaint.setStyle(Paint.Style.STROKE);
                defaultPaint.setStrokeWidth(6);
                Resources res = getResources();
                int defalt = res.getColor(R.color.defalt);
                defaultPaint.setColor(defalt);
                rectF = new RectF(10, 15, 80, 85);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                p.setAntiAlias(true);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(6);

                if (localInt >= 270) {
                    p.setColor(getResources().getColor(R.color.green));
                } else if (localInt >= 180) {
                    p.setColor(getResources().getColor(R.color.orange));
                } else if (localInt > 0) {
                    p.setColor(getResources().getColor(R.color.red));
                }
                canvas.drawArc(rectF, 0, 360, false, defaultPaint);
                canvas.drawArc(rectF, 270, Float.parseFloat(localInt + ""), false, p);
            }
        }
    }
}