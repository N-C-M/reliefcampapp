package xyz.appmaker.keralarescue.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.appmaker.keralarescue.AppController;
import xyz.appmaker.keralarescue.MainActivity;
import xyz.appmaker.keralarescue.Models.Gender;
import xyz.appmaker.keralarescue.Models.PersonsResponse;
import xyz.appmaker.keralarescue.Models.States;
import xyz.appmaker.keralarescue.R;
import xyz.appmaker.keralarescue.Room.Camp.CampNames;
import xyz.appmaker.keralarescue.Room.CampDatabase;
import xyz.appmaker.keralarescue.Room.PersonData.PersonDataDao;
import xyz.appmaker.keralarescue.Room.PersonData.PersonDataEntity;
import xyz.appmaker.keralarescue.Tools.APIService;
import xyz.appmaker.keralarescue.Tools.PreferensHandler;

public class FieldsActivity extends AppCompatActivity {


    EditText nameEdt, ageEdt, addressEdt, mobileEdt, notesEdt;
    TextView syncDetailsTextView;
    Spinner campNameSpn, genderSpn;//, districtSpn;
    HashMap<String, String> distMap = new HashMap<>();
    PreferensHandler pref;
    Button submitBtn;
    Context context;
    private PersonDataDao personDao;
    CampDatabase dbInstance;

    ArrayList<Gender> genderList = new ArrayList<>();
    //ArrayList<CampNames> campList = new ArrayList<>();

    String districtSelectedValue = "tvm";
    String genderSelectedValue = "0";
    String campSelectedValue = "0";
    APIService apiService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fields);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        context = getApplicationContext();
        pref = new PreferensHandler(context);
        syncDetailsTextView = findViewById(R.id.syncDetails);
        dbInstance = CampDatabase.getDatabase(context);
        apiService = AppController.getRetrofitInstance();


        /*findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent fieldClass = new Intent(FieldsActivity.this, CampsActivity.class);
                startActivity(fieldClass);
            }
        });*/

        //Camp Spinners
        //campNameSpn = findViewById(R.id.camp_spinner);


        // Gender spinner
        genderList.add(new Gender("0", "Male"));
        genderList.add(new Gender("1", "Female"));
        genderList.add(new Gender("2", "Others"));

        ArrayAdapter<Gender> genderAdapter = new ArrayAdapter<Gender>(this,
                android.R.layout.simple_spinner_item, genderList);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpn = (Spinner) findViewById(R.id.gender);
        genderSpn.setAdapter(genderAdapter);


        genderSpn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //stateSelectedValue
                Gender gender = (Gender) parent.getSelectedItem();

                genderSelectedValue = gender.getId();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });




       /* if (pref != null)
            districtSpn.setSelection(pref.getDistrictDef());

        districtSpn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (pref != null)
                    pref.setDistrictDef(position);
                States states = (States) parent.getSelectedItem();
                districtSelectedValue = states.getId();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });*/


        nameEdt = (EditText) findViewById(R.id.name);
        ageEdt = (EditText) findViewById(R.id.age);
        addressEdt = (EditText) findViewById(R.id.address);
        mobileEdt = (EditText) findViewById(R.id.mobile);
        notesEdt = (EditText) findViewById(R.id.note);
        submitBtn = (Button) findViewById(R.id.submit);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("Tag", "state code, gender code " + districtSelectedValue + "  - " + genderSelectedValue);

                if (validateData()) {
                    PersonDataEntity personDataModel = new PersonDataEntity(
                            nameEdt.getText().toString(),
                            campSelectedValue,
                            ageEdt.getText().toString(),
                            genderSelectedValue,
                            addressEdt.getText().toString(),
                            districtSelectedValue,
                            mobileEdt.getText().toString(),
                            notesEdt.getText().toString(),
                            "0");
                    insetPersonDb(personDataModel);
                }
            }
        });
       // updateCamps();
       // loadCamps();

        syncDB();
        updateSynced();
    }

    public void updateSynced() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Integer suyncedCount = dbInstance.personDataDao().statusCount("1");
                final Integer unsyncedCount = dbInstance.personDataDao().statusCount("0");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        syncDetailsTextView.setText(unsyncedCount + " Pending - " + suyncedCount + " Synced");
                    }
                });
            }
        }).start();

    }


    public void syncDB() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<PersonDataEntity> personDataUnsynced = dbInstance.personDataDao().getUnSyncedPersons();
                apiService.addPersons(authToken(), personDataUnsynced).enqueue(new Callback<PersonsResponse>() {
                    @Override
                    public void onResponse(Call<PersonsResponse> call, Response<PersonsResponse> response) {

                        if (response.isSuccessful()) {
                            final int[] updateIds = new int[200];
                            int index = 0;
                            for (PersonDataEntity personDataEntity : personDataUnsynced) {
                                updateIds[index++] = personDataEntity.id;
                            }
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    dbInstance.personDataDao().updateStatus(updateIds, "1");
                                    updateSynced();
                                }
                            }).start();
                        } else {
                            Toast.makeText(getApplicationContext(), "Some error while saving data, Please contact admin", Toast.LENGTH_LONG).show();
                            updateSynced();
                        }
                    }

                    @Override
                    public void onFailure(Call<PersonsResponse> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
                        updateSynced();
                    }
                });

            }
        }).start();
    }

    public void loadPersons() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<PersonDataEntity> personDataEntities = dbInstance.personDataDao().getAllPersons();
                PersonDataEntity personDataEntity = personDataEntities.get(0);

                List<PersonDataEntity> personDataUnsyncedEntities = dbInstance.personDataDao().getUnSyncedPersons();
                PersonDataEntity personDataUnsyncedEntitie = personDataEntities.get(0);
            }
        }).start();
    }

   /* public void loadCamps() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                campList = (ArrayList<CampNames>) dbInstance.campDao().getAllCamps();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayAdapter<CampNames> campListArrayAdapter = new ArrayAdapter<CampNames>(FieldsActivity.this,
                                android.R.layout.simple_spinner_item, campList);
                        if (campList.size() > 0)
                            campSelectedValue = String.valueOf(campList.get(0).getId());
                        campListArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        campNameSpn.setAdapter(campListArrayAdapter);
                        campNameSpn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                //stateSelectedValue
                                CampNames campName = (CampNames) parent.getSelectedItem();
                                campSelectedValue = String.valueOf(campName.getId());
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                    }
                });
            }
        }).start();

    }*/

    public boolean validateData() {
        if (nameEdt.getText().toString().equals("") || ageEdt.getText().toString().equals("")) {
//            || ageEdt.getText().toString().equals("") || addressEdt.getText().toString().equals("") ||
//                    mobileEdt.getText().toString().equals("") || notesEdt.getText().toString().equals("")
            Toast.makeText(context, "Name and age is required",
                    Toast.LENGTH_LONG).show();
            return false;
        } else {
            return true;
        }
    }

    public void insetPersonDb(final PersonDataEntity var) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                dbInstance.personDataDao().insert(var);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        personAdded();
                        syncDB();
                    }
                });


            }
        }).start();
    }

    private void personAdded() {
        Toast.makeText(context, "Person added successfully",
                Toast.LENGTH_LONG).show();
        nameEdt.setText("");
        ageEdt.setText("");
        addressEdt.setText("");
        mobileEdt.setText("");
        notesEdt.setText("");
//        nameEdt.setText("");

    }

    public String authToken() {
        return "JWT " + pref.getUserToken();
    }

   /* public void updateCamps() {
        Call<List<CampNames>> response = apiService.getCampList(authToken());
        response.enqueue(new Callback<List<CampNames>>() {
            @Override
            public void onResponse(Call<List<CampNames>> call, Response<List<CampNames>> response) {
                Log.e("TAG", "success response ");

                List<CampNames> items = response.body();

                if (items != null && items.size() > 0) {
                    String name = items.get(0).getName();
                    Toast.makeText(getApplicationContext(), name, Toast.LENGTH_LONG).show();
                    insetCampDb(items);
                }
            }

            @Override
            public void onFailure(Call<List<CampNames>> call, Throwable t) {
                Log.e("TAG", "fail response ");

                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
*/
   /* public void insetCampDb(final List<CampNames> var) {
        Log.e("TAG", "insetCampDb ");

        new Thread(new Runnable() {
            @Override
            public void run() {
                dbInstance.campDao().insertCapms(var);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //loadCamps();
                    }
                });
            }
        }).start();
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sync) {
            Toast.makeText(this, "Syncing", Toast.LENGTH_SHORT).show();
            syncDB();

            return true;
        }

        if (id == R.id.action_logout) {
            Toast.makeText(this, "Loging out", Toast.LENGTH_SHORT).show();
            logoutUser();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public  void logoutUser(){
        if(pref!= null){
            pref.setUserToken("");
            Intent actLogin = new Intent(FieldsActivity.this, MainActivity.class);
            startActivity(actLogin);
        }
    }
}
